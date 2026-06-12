package com.fundflow.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * Join entity between an Investor (LP) and a Fund: how much capital the LP
 * has committed to the fund. Capital calls are split pro-rata by these amounts.
 */
@Entity
@Table(name = "commitment", uniqueConstraints = {
        @UniqueConstraint(name = "uq_commitment_fund_investor", columnNames = {"fund_id", "investor_id"})
})
public class Commitment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fund_id", nullable = false)
    private Fund fund;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "investor_id", nullable = false)
    private Investor investor;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    protected Commitment() {
        // JPA
    }

    public Commitment(Fund fund, Investor investor, BigDecimal amount) {
        this.fund = fund;
        this.investor = investor;
        this.amount = amount;
    }

    public Long getId() {
        return id;
    }

    public Fund getFund() {
        return fund;
    }

    public Investor getInvestor() {
        return investor;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
