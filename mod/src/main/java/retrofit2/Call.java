package retrofit2;

public interface Call<T> {
    void enqueue(Callback<T> callback);
    retrofit2.Response<T> execute() throws java.io.IOException;
    boolean isExecuted();
    void cancel();
    boolean isCanceled();
    Call<T> clone();
    interface Factory {
        Call<?> create(retrofit2.Request request);
    }
}