package raft.common;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Objects;

@Getter
@Setter
@Builder
@ToString
public class Peer {

    // ip:port
    private String addr;

    public Peer(String addr) {
        this.addr = addr;
    }

    public static String getIP(String addr) {
        return addr.split(":")[0];
    }

    public static int getPort(String addr) {
        return Integer.parseInt(addr.split(":")[1]);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Peer peer = (Peer) o;
        return Objects.equals(addr, peer.addr);
    }

    @Override
    public int hashCode() {

        return Objects.hash(addr);
    }


}
