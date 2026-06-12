package com.fundflow.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "capital_call", uniqueConstraints = {
        @UniqueConstraint(name = "uq_capital_call_fund_number", columnNames = {"fund_id", "call_number"})
})
public class CapitalCall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fund_id", nullable = false)
    private Fund fund;

    @Column(name = "call_number", nullable = false)
    private int callNumber;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CallStatus status;

    @OneToMany(mappedBy = "capitalCall", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CallAllocation> allocations = new ArrayList<>();

    protected CapitalCall() {
        // JPA
    }

    public CapitalCall(Fund fund, int callNumber, BigDecimal totalAmount, LocalDate dueDate) {
        this.fund = fund;
        this.callNumber = callNumber;
        this.totalAmount = totalAmount;
        this.dueDate = dueDate;
        this.status = CallStatus.DRAFT;
    }

    public void addAllocation(CallAllocation allocation) {
        allocations.add(allocation);
    }

    public Long getId() {
        return id;
    }

    public Fund getFund() {
        return fund;
    }

    public int getCallNumber() {
        return callNumber;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public CallStatus getStatus() {
        return status;
    }

    public void setStatus(CallStatus status) {
        this.status = status;
    }

    public List<CallAllocation> getAllocations() {
        return allocations;
    }
}
