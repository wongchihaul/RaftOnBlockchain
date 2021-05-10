package raft.entity;


import chainUtils.NoobChain;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
//@Builder
public class LogEntry implements Serializable, Comparable {

    private long term;

    private long index;

    private Transaction transaction;

    private NoobChain noobChain;

    public LogEntry(){

    }

    public LogEntry(long term, long index, Transaction transaction, NoobChain noobChain) {
        this.term = term;
        this.index = index;
        this.transaction = transaction;
        this.noobChain = noobChain;
    }

    public LogEntry(long term, Transaction transaction) {
        this.term = term;
        this.transaction = transaction;
    }
    public LogEntry(long index,long term, Transaction transaction) {
        this.index = index;
        this.term = term;
        this.transaction = transaction;
    }

    @Override
    public int compareTo(Object o) {
        if (o == null) {
            return -1;
        }
        if (this.getIndex() > ((LogEntry) o).getIndex()) {
            return 1;
        } else {
            return -1;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        LogEntry logEntry = (LogEntry) o;
        return term == logEntry.getTerm() &&
                index == logEntry.getIndex() &&
                transaction.equals(logEntry.getTransaction());
    }

    @Override
    public int hashCode() {
        return Objects.hash(term, index, transaction);
    }

}
