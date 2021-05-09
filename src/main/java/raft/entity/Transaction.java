package raft.entity;

import chainUtils.NoobChain;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Objects;

/**
 * Commands used in DAO
 */

@Getter
@Setter
@ToString
@Builder
public class Transaction implements Serializable {

    public String key;
    public NoobChain noobChain;
    public String value;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        Transaction transaction = (Transaction) o;
        return key.equals(transaction.key) &&
                value.equals(transaction.value) &&
                noobChain.equals((transaction.noobChain));
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }
}