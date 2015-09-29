package play.it.http.websocket;

import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import play.libs.F;
import play.mvc.Results;
import play.mvc.WebSocket;
import scala.compat.java8.FutureConverters;
import scala.concurrent.Promise;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Java actions for WebSocket spec
 */
public class WebSocketSpecJavaActions {

    private static <A> Sink<A, ?> getChunks(Consumer<List<A>> onDone) {
        return Sink.<List<A>, A>fold(new ArrayList<A>(), (result, next) -> {
            result.add(next);
            return result;
        }).mapMaterializedValue(future -> FutureConverters.toJava(future).thenAccept(onDone));
    }

    private static <A> Source<A, ?> emptySource() {
        return Source.from(FutureConverters.toScala(new CompletableFuture<>()));
    }

    public static WebSocket allowConsumingMessages(Promise<List<String>> messages) {
        return WebSocket.Text.accept(request -> Flow.wrap(getChunks(messages::success), emptySource(), Keep.none()));
    }

    public static WebSocket allowSendingMessages(List<String> messages) {
        return WebSocket.Text.accept(request -> Flow.wrap(Sink.ignore(), Source.from(messages), Keep.none()));
    }

    public static WebSocket closeWhenTheConsumerIsDone() {
        return WebSocket.Text.accept(request -> Flow.wrap(Sink.cancelled(), emptySource(), Keep.none()));
    }

    public static WebSocket allowRejectingAWebSocketWithAResult(int statusCode) {
        return WebSocket.Text.acceptOrResult(request -> CompletableFuture.completedFuture(F.Either.Left(Results.status(statusCode))));
    }


}