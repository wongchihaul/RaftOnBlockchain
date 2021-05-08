package client;

import java.io.Serializable;
import chainUtils.NoobChain;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 *
 * @author 莫那·鲁道
 */
@Getter
@Setter
@ToString
@Builder
public class KVReq implements Serializable {

    public static int PUT = 0;
    public static int GET = 1;

    int type;
    NoobChain noobChain;
    String key;

    String value;

//    private KVReq(Builder builder) {
//        setType(builder.type);
//        setKey(builder.key);
//        setNoobChain(builder.noobChain);
//        setValue(builder.value);
//    }

//    public static Builder newBuilder() {
//        return new Builder();
//    }
//
//    public enum Type {
//        PUT(0), GET(1);
//        int code;
//
//        Type(int code) {
//            this.code = code;
//        }
//
//        public static Type value(int code ) {
//            for (Type type : values()) {
//                if (type.code == code) {
//                    return type;
//                }
//            }
//            return null;
//        }
//    }
//
//
//    public static final class Builder {
//
//        private int type;
//        private String key;
//        private String value;
//        private NoobChain noobChain;
//        private Builder() {
//        }
//
//
//        public Builder type(int val) {
//            type = val;
//            return this;
//        }
//        public Builder noobChain(NoobChain val) {
//            noobChain = val;
//            return this;
//        }
//
//        public Builder key(String val) {
//            key = val;
//            return this;
//        }
//
//        public Builder value(String val) {
//            value = val;
//            return this;
//        }
//
//        public KVReq build() {
//            return new KVReq(this);
//        }
    //}
}