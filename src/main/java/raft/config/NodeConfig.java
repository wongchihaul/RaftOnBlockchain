package raft.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;


@Getter
@Setter
@ToString
public class NodeConfig {

    //self port
    public int port;

    // address of all peers
    public List<String> peers;

}