package com.reimo.reimohelper.event;

import com.reimo.reimohelper.failsafe.FailsafeManager;
import net.minecraftforge.client.event.SystemMessageReceivedEvent;

public class ChatEventHandler {
    public static void onSystemMessage(SystemMessageReceivedEvent event) {
        if (event == null || event.getMessage() == null) return;
        FailsafeManager.getInstance().onSystemMessage(event.getMessage().getString());
    }
}
