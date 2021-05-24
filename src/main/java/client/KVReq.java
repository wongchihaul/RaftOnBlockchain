package client;

import chainUtils.NoobChain;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.ArrayList;


@Getter
@Setter
@ToString
@Builder
public class KVReq implements Serializable {

    public static int PUT = 0;
    public static int GET = 1;

    int type;
    NoobChain noobChain;
    ArrayList<String> key;
    String reqKey;
    ArrayList<String> value;
    boolean demoVersion = false;
}