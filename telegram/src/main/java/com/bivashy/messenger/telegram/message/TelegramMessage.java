package com.bivashy.messenger.telegram.message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.bivashy.messenger.common.ApiProvider;
import com.bivashy.messenger.common.file.MessengerFile;
import com.bivashy.messenger.common.identificator.Identificator;
import com.bivashy.messenger.common.message.DefaultMessage;
import com.bivashy.messenger.telegram.message.keyboard.TelegramKeyboard;
import com.bivashy.messenger.telegram.providers.TelegramApiProvider;
import com.pengrad.telegrambot.Callback;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.request.SendAudio;
import com.pengrad.telegrambot.request.SendDocument;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import com.pengrad.telegrambot.request.SendVideo;
import com.pengrad.telegrambot.response.SendResponse;

public class TelegramMessage extends DefaultMessage {
    private static TelegramApiProvider defaultApiProvider;

    public TelegramMessage(String text) {
        super(text);
    }

    public static TelegramApiProvider getDefaultApiProvider() {
        return defaultApiProvider;
    }

    public static void setDefaultApiProvider(TelegramApiProvider defaultApiProvider) {
        TelegramMessage.defaultApiProvider = defaultApiProvider;
    }

    @Override
    public void send(Identificator identificator) {
        if (defaultApiProvider == null)
            throw new NullPointerException(
                    "Default telegram api provider was not defined. Define with static TelegramMessage#setDefaultApiProvider method!");
        send(identificator, defaultApiProvider);
    }

    public void send(Identificator identificator, Consumer<SendResponse> responseConsumer) {
        if (defaultApiProvider == null)
            throw new NullPointerException(
                    "Default telegram api provider was not defined. Define with static TelegramMessage#setDefaultApiProvider method!");
        send(identificator, defaultApiProvider, responseConsumer);
    }

    @Override
    public void send(Identificator identificator, ApiProvider apiProvider) {
        send(identificator, apiProvider, (ignored) -> {
        });
    }

    public void send(Identificator identificator, ApiProvider apiProvider, Consumer<SendResponse> responseConsumer) {
        SendMessage sendMessage = new SendMessage(identificator.asObject(), text);
        if (keyboard != null && keyboard.safeAs(TelegramKeyboard.class).isPresent())
            sendMessage.replyMarkup(keyboard.as(TelegramKeyboard.class).create());
        if (replyIdentificator != null && replyIdentificator.isNumber())
            sendMessage.replyToMessageId((int) replyIdentificator.asNumber());
        apiProvider.as(TelegramApiProvider.class).getBot().execute(sendMessage,
                new Callback<SendMessage, SendResponse>() {
                    @Override
                    public void onResponse(SendMessage request, SendResponse response) {
                        responseConsumer.accept(response);
                    }

                    @Override
                    public void onFailure(SendMessage request, IOException e) {
                        e.printStackTrace();
                    }
                });
        if (files != null && files.length != 0)
            toEntities(identificator, files)
                    .forEach(request -> apiProvider.as(TelegramApiProvider.class).getBot().execute(request));
    }

    private List<BaseRequest<?, ?>> toEntities(Identificator identificator, MessengerFile... files) {
        List<BaseRequest<?, ?>> requests = new ArrayList<>();
        for (MessengerFile file : files) {
            BaseRequest<?, ?> request = toRequest(identificator, file);
            if (request != null)
                requests.add(request);
        }
        return requests;
    }

    private BaseRequest<?, ?> toRequest(Identificator identificator, MessengerFile file) {
        switch (file.getFileType()) {
            case AUDIO:
                return new SendAudio(identificator.asObject(), file.getFile());
            case DOCUMENT:
                return new SendDocument(identificator.asObject(), file.getFile());
            case PHOTO:
                return new SendPhoto(identificator.asObject(), file.getFile());
            case VIDEO:
                return new SendVideo(identificator.asObject(), file.getFile());
            case OTHER:
            default:
                break;
        }
        return null;
    }

    public static class Builder extends DefaultMessageBuilder {
        public Builder(String text) {
            super(new TelegramMessage(text));
        }
    }
}
