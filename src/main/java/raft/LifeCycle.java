package raft;

public interface LifeCycle {

    void init() throws Exception;

    void destroy() throws Exception;
}
