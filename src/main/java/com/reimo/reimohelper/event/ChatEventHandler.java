package com.reimo.reimohelper.event;

import com.reimo.reimohelper.failsafe.FailsafeManager;
import net.minecraftforge.client.event.SystemMessageReceivedEvent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;

public class ChatEventHandler {
    public static void onSystemMessage(SystemMessageReceivedEvent event) {
        if (event == null || event.getMessage() == null) return;
        FailsafeManager.getInstance().onSystemMessage(event.getMessage().getString());
    }

    // also listen to normal chat messages (e.g. server broadcasts, command blocks)
    public static void onChat(ClientChatReceivedEvent event) {
        if (event == null || event.getMessage() == null) return;
        FailsafeManager.getInstance().onSystemMessage(event.getMessage().getString());
    }
}
