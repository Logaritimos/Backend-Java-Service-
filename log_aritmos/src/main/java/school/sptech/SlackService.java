package school.sptech;

import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;

public class SlackService {

    private final Slack slack = Slack.getInstance();

    public void enviarMensagem(String token, String canalId, String mensagem) {
        try {
            MethodsClient methods = slack.methods(token);

            ChatPostMessageRequest request = ChatPostMessageRequest.builder()
                    .channel(canalId)
                    .text(mensagem)
                    .build();

            ChatPostMessageResponse response = methods.chatPostMessage(request);

            if (response.isOk()) {
                System.out.println("Slack: Mensagem enviada com sucesso para " + canalId);
            } else {
                System.err.println("Erro Slack: " + response.getError());
            }
        } catch (Exception e) {
            System.err.println("Erro de conexão com Slack: " + e.getMessage());
        }
    }
}