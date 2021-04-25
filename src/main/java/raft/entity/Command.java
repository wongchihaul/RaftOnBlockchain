package raft.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

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

    public String value;

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
                value.equals(command.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }
}
