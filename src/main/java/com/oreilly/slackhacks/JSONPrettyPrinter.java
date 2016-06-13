package com.oreilly.slackhacks;

import com.cedarsoftware.util.io.JsonWriter;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class JSONPrettyPrinter
{
    private static final String TOKEN = "insert_here_your_bot_token";

    public static void main(String [] args) throws Exception
    {
        SlackSession session = SlackSessionFactory.createWebSocketSlackSession(TOKEN);
        session.addMessagePostedListener(JSONPrettyPrinter::processEvent);
        session.connect();
        Thread.sleep(Long.MAX_VALUE);
    }

    private static void processEvent(SlackMessagePosted event, SlackSession session) {
        if (isFileSentFromAnotherUser(event, session))
        {
            return;
        }

        //trying to pretty print the file
        try
        {
            sendBackPrettifiedFile(event, session, formatJson(downloadFile(event)));

        }
        catch (Exception e)
        {
            failOnException(event, session, e);
        }
    }

    private static void failOnException(SlackMessagePosted event, SlackSession session, Exception e)
    {
        session.sendMessage(event.getChannel(),"Sorry I was unable to process your file : " + e.getMessage());
    }

    private static boolean isFileSentFromAnotherUser(SlackMessagePosted event, SlackSession session)
    {
        //if the event is not on a direct channel
        if (!event.getChannel().isDirect()) {
            return true;
        }
        //if the event was triggered by the bot
        if (event.getSender().getId().equals(session.sessionPersona().getId())) {
            return true;
        }
        //if the event doesn't contain a file
        if (event.getSlackFile() == null) {
            return true;
        }
        return false;
    }

    private static void sendBackPrettifiedFile(SlackMessagePosted _event, SlackSession _session, String formattedJson)
    {
        _session.sendFile(_event.getChannel(),formattedJson.getBytes(), "pretty_"+_event.getSlackFile().getName());
    }

    private static String formatJson(String jsonString)
    {
        return JsonWriter.formatJson(jsonString);
    }

    private static String downloadFile(SlackMessagePosted event) throws IOException
    {
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet(event.getSlackFile().getUrlPrivate());
        get.setHeader("Authorization", "Bearer " + TOKEN);
        HttpResponse response =  client.execute(get);
        BufferedReader buffer = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        return buffer.lines().collect(Collectors.joining("\n"));
    }


}
