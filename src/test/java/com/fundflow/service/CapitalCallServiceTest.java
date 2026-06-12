package com.fundflow.service;

import com.fundflow.domain.*;
import com.fundflow.exception.BusinessRuleException;
import com.fundflow.repository.CallAllocationRepository;
import com.fundflow.repository.CapitalCallRepository;
import com.fundflow.repository.CommitmentRepository;
import com.fundflow.repository.FundRepository;
import com.fundflow.web.dto.CapitalCallDtos.AllocationResponse;
import com.fundflow.web.dto.CapitalCallDtos.CapitalCallRequest;
import com.fundflow.web.dto.CapitalCallDtos.CapitalCallResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for the pro-rata allocation engine and the capital call
 * state machine. Repositories are mocked; no Spring context, no database.
 */
@ExtendWith(MockitoExtension.class)
class CapitalCallServiceTest {

    private static final LocalDate DUE_DATE = LocalDate.of(2030, 1, 15);

    @Mock
    private CapitalCallRepository capitalCallRepository;
    @Mock
    private CallAllocationRepository allocationRepository;
    @Mock
    private CommitmentRepository commitmentRepository;
    @Mock
    private FundRepository fundRepository;

    private CapitalCallService service;
    private Fund fund;

    @BeforeEach
    void setUp() {
        service = new CapitalCallService(capitalCallRepository, allocationRepository,
                commitmentRepository, fundRepository);
        fund = new Fund("Test Fund I", 2024, new BigDecimal("50000000"), "USD");
    }

    private void stubFundWithCommitments(List<Commitment> commitments) {
        when(fundRepository.findById(1L)).thenReturn(Optional.of(fund));
        when(commitmentRepository.findByFundId(1L)).thenReturn(commitments);
        lenient().when(capitalCallRepository.findTopByFundIdOrderByCallNumberDesc(1L))
                .thenReturn(Optional.empty());
        lenient().when(capitalCallRepository.save(any(CapitalCall.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Commitment commitment(String investorName, String amount) {
        return new Commitment(fund, new Investor(investorName, investorName + "@lp.example"),
                new BigDecimal(amount));
    }

    @Nested
    class ProRataAllocation {

        @Test
        void splitsExactlyByCommitmentShare() {
            stubFundWithCommitments(List.of(
                    commitment("Meridian", "20000000"),
                    commitment("Cypress", "10000000"),
                    commitment("BlueHarbor", "5000000")));

            CapitalCallResponse response = service.createWithAllocations(1L,
                    new CapitalCallRequest(new BigDecimal("7000000"), DUE_DATE));

            Map<String, BigDecimal> byInvestor = response.allocations().stream()
                    .collect(Collectors.toMap(AllocationResponse::investorName, AllocationResponse::amount));
            assertThat(byInvestor.get("Meridian")).isEqualByComparingTo("4000000.00");
            assertThat(byInvestor.get("Cypress")).isEqualByComparingTo("2000000.00");
            assertThat(byInvestor.get("BlueHarbor")).isEqualByComparingTo("1000000.00");
        }

        @Test
        void roundingRemainderGoesToLargestCommitment() {
            // 100.00 across three equal thirds cannot split evenly:
            // two LPs owe 33.33, the largest commitment absorbs the extra cent.
            stubFundWithCommitments(List.of(
                    commitment("A", "1000000"),
                    commitment("B", "1000000"),
                    commitment("C", "1000000.01")));

            CapitalCallResponse response = service.createWithAllocations(1L,
                    new CapitalCallRequest(new BigDecimal("100.00"), DUE_DATE));

            Map<String, BigDecimal> byInvestor = response.allocations().stream()
                    .collect(Collectors.toMap(AllocationResponse::investorName, AllocationResponse::amount));
            assertThat(byInvestor.get("A")).isEqualByComparingTo("33.33");
            assertThat(byInvestor.get("B")).isEqualByComparingTo("33.33");
            assertThat(byInvestor.get("C")).isEqualByComparingTo("33.34");
        }

        @ParameterizedTest(name = "total={0}")
        @CsvSource({
                "100.00",
                "999999.97",
                "0.01",
                "12345678.90",
                "7000000.00"
        })
        void allocationsAlwaysSumExactlyToCallTotal(String total) {
            stubFundWithCommitments(List.of(
                    commitment("A", "3333333.33"),
                    commitment("B", "1111111.11"),
                    commitment("C", "777777.77"),
                    commitment("D", "13.13"),
                    commitment("E", "9999999.99")));

            CapitalCallResponse response = service.createWithAllocations(1L,
                    new CapitalCallRequest(new BigDecimal(total), DUE_DATE));

            BigDecimal sum = response.allocations().stream()
                    .map(AllocationResponse::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(sum).isEqualByComparingTo(total);
        }

        @Test
        void singleInvestorReceivesFullAmount() {
            stubFundWithCommitments(List.of(commitment("Solo", "5000000")));

            CapitalCallResponse response = service.createWithAllocations(1L,
                    new CapitalCallRequest(new BigDecimal("250000.50"), DUE_DATE));

            assertThat(response.allocations()).hasSize(1);
            assertThat(response.allocations().get(0).amount()).isEqualByComparingTo("250000.50");
        }

        @Test
        void rejectsCallWhenFundHasNoCommitments() {
            when(fundRepository.findById(1L)).thenReturn(Optional.of(fund));
            when(commitmentRepository.findByFundId(1L)).thenReturn(List.of());

            assertThatThrownBy(() -> service.createWithAllocations(1L,
                    new CapitalCallRequest(new BigDecimal("100"), DUE_DATE)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("no commitments");
        }

        @Test
        void callNumberIncrementsFromLatestCall() {
            stubFundWithCommitments(List.of(commitment("A", "1000000")));
            when(capitalCallRepository.findTopByFundIdOrderByCallNumberDesc(1L))
                    .thenReturn(Optional.of(new CapitalCall(fund, 3, BigDecimal.TEN, DUE_DATE)));

            CapitalCallResponse response = service.createWithAllocations(1L,
                    new CapitalCallRequest(new BigDecimal("100"), DUE_DATE));

            assertThat(response.callNumber()).isEqualTo(4);
        }

        @Test
        void newCallStartsAsDraft() {
            stubFundWithCommitments(List.of(commitment("A", "1000000")));

            CapitalCallResponse response = service.createWithAllocations(1L,
                    new CapitalCallRequest(new BigDecimal("100"), DUE_DATE));

            assertThat(response.status()).isEqualTo(CallStatus.DRAFT);
        }
    }

    @Nested
    class StateMachine {

        private CapitalCall callWithTwoAllocations() {
            CapitalCall call = new CapitalCall(fund, 1, new BigDecimal("1000"), DUE_DATE);
            call.addAllocation(new CallAllocation(call, new Investor("A", "a@lp.example"), new BigDecimal("600")));
            call.addAllocation(new CallAllocation(call, new Investor("B", "b@lp.example"), new BigDecimal("400")));
            return call;
        }

        @Test
        void issueMovesDraftToIssued() {
            CapitalCall call = callWithTwoAllocations();
            when(capitalCallRepository.findById(1L)).thenReturn(Optional.of(call));

            assertThat(service.issue(1L).status()).isEqualTo(CallStatus.ISSUED);
        }

        @Test
        void issueRejectsNonDraftCall() {
            CapitalCall call = callWithTwoAllocations();
            call.setStatus(CallStatus.ISSUED);
            when(capitalCallRepository.findById(1L)).thenReturn(Optional.of(call));

            assertThatThrownBy(() -> service.issue(1L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("cannot be issued");
        }

        @Test
        void payingOneOfTwoAllocationsMarksCallPartiallyPaid() {
            CapitalCall call = callWithTwoAllocations();
            call.setStatus(CallStatus.ISSUED);
            when(allocationRepository.findById(anyLong()))
                    .thenReturn(Optional.of(call.getAllocations().get(0)));

            AllocationResponse paid = service.pay(10L);

            assertThat(paid.paid()).isTrue();
            assertThat(paid.paidAt()).isNotNull();
            assertThat(call.getStatus()).isEqualTo(CallStatus.PARTIALLY_PAID);
        }

        @Test
        void payingAllAllocationsMarksCallPaid() {
            CapitalCall call = callWithTwoAllocations();
            call.setStatus(CallStatus.ISSUED);
            when(allocationRepository.findById(10L)).thenReturn(Optional.of(call.getAllocations().get(0)));
            when(allocationRepository.findById(11L)).thenReturn(Optional.of(call.getAllocations().get(1)));

            service.pay(10L);
            service.pay(11L);

            assertThat(call.getStatus()).isEqualTo(CallStatus.PAID);
        }

        @Test
        void cannotPayAllocationOfDraftCall() {
            CapitalCall call = callWithTwoAllocations();
            when(allocationRepository.findById(anyLong()))
                    .thenReturn(Optional.of(call.getAllocations().get(0)));

            assertThatThrownBy(() -> service.pay(10L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("issue it first");
        }

        @Test
        void cannotPayTwice() {
            CapitalCall call = callWithTwoAllocations();
            call.setStatus(CallStatus.ISSUED);
            when(allocationRepository.findById(anyLong()))
                    .thenReturn(Optional.of(call.getAllocations().get(0)));

            service.pay(10L);

            assertThatThrownBy(() -> service.pay(10L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("already paid");
        }
    }
}
