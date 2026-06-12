package com.fundflow.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One investor's pro-rata share of a capital call.
 */
@Entity
@Table(name = "call_allocation", uniqueConstraints = {
        @UniqueConstraint(name = "uq_allocation_call_investor", columnNames = {"capital_call_id", "investor_id"})
})
public class CallAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "capital_call_id", nullable = false)
    private CapitalCall capitalCall;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "investor_id", nullable = false)
    private Investor investor;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private boolean paid;

    @Column(name = "paid_at")
    private Instant paidAt;

    protected CallAllocation() {
        // JPA
    }

    public CallAllocation(CapitalCall capitalCall, Investor investor, BigDecimal amount) {
        this.capitalCall = capitalCall;
        this.investor = investor;
        this.amount = amount;
        this.paid = false;
    }

    public void markPaid() {
        this.paid = true;
        this.paidAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public CapitalCall getCapitalCall() {
        return capitalCall;
    }

    public Investor getInvestor() {
        return investor;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public boolean isPaid() {
        return paid;
    }

    public Instant getPaidAt() {
        return paidAt;
    }
}
