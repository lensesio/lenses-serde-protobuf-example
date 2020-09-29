package io.lenses.examples.serde.domain;

import java.io.Serializable;

public class CreditCard implements Serializable {
    private final String name;
    private final String country;
    private final String currency;
    private final String cardNumber;
    private final Boolean blocked;
    private final CardType type;

    public CreditCard(String name, String country, String currency, String cardNumber, Boolean blocked, CardType type) {
        this.name = name;
        this.country = country;
        this.currency = currency;
        this.cardNumber = cardNumber;
        this.blocked = blocked;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getCountry() {
        return country;
    }

    public String getCurrency() {
        return currency;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public Boolean getBlocked() {
        return blocked;
    }

    public CardType getType() {
        return type;
    }
}
