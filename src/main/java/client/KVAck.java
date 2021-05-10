package client;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Builder
public class KVAck implements Serializable {

    boolean success;

    String val;
}
