package raft.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import chainUtils.NoobChain;

import java.util.Objects;

/**
 * Commands used in DAO
 */

@Getter
@Setter
@ToString
@Builder
public class Command {

    public String key;
    public NoobChain noobChain;
    public String value;
    public Command(String key, String value,NoobChain noobChain) {
        this.key = key;
        this.value = value;
        this.noobChain=noobChain;
    }

    private Command(Builder builder) {
        setKey(builder.key);
        setNoobChain(builder.noobChain);
        setValue(builder.value);
    }

    public static Builder newBuilder() {
        return new Builder();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        Command command = (Command) o;
        return key.equals(command.key) &&
                value.equals(command.value)&&
                noobChain.equals((command.noobChain));
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }
    public static final class Builder {

        private String key;
        private String value;
        private NoobChain noobChain;
        private Builder() {
        }

        public Builder key(String val) {
            key = val;
            return this;
        }
        public Builder noobChain(NoobChain val) {
            noobChain = val;
            return this;
        }

        public Builder value(String val) {
            value = val;
            return this;
        }

        public Command build() {
            return new Command(this);
        }


    }
}