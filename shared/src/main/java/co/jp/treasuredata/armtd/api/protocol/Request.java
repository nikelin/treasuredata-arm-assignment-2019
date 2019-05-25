package co.jp.treasuredata.armtd.api.protocol;

public final class Request {
    private final int token;
    private final String typeName;
    private final String[] arguments;

    public Request(int token, String typeName, String[] arguments) {
        this.token = token;
        this.typeName = typeName;
        this.arguments = arguments;
    }

    public int getToken() {
        return token;
    }

    public String getTypeName() {
        return typeName;
    }

    public String[] getArguments() {
        return arguments;
    }
}