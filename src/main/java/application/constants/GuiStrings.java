package application.constants;

public class GuiStrings {
    public static final String BROKER_TAB_NAME = "Brokers";
    public static final String TOPIC_CONFIGS_TAB_NAME = "Topic configs";
    public static final String SENDER_MSG_TAB_NAME = "Send msg";
    public static final String LISTENER_MSG_TAB_NAME = "Receive msg";
    public static final String GROOVY_SCRIPTING_TAB_NAME = "Execute script before sending message";
    public static final String BEFORE_FIRST_MSGS_SHARED_SCRIPT_TAB_NAME = "Before FIRST message [shared]";
    public static final String BEFORE_EACH_MSGS_SCRIPT_TAB = "Before EACH message";
    public static final String BEFORE_FIRST_MSGS_SCRIPT_TAB_NAME = "Before FIRST message";
    public static final String MESSAGE_BODY_TEMPLATE_NAME = "Message body";
    public static final String REPEAT_COUNT_LABEL_TEXT = "Repeat count";
    public static final String SEND_BUTTON_TEXT = "Send";
    public static final String VAR_NAME = "cat_age";
    public static final String MESSAGE_DEFINITION_TOOL_TIP = "'" + MESSAGE_BODY_TEMPLATE_NAME
            + "' tab holds the message body that will be sent to kafka broker\n\rexample :\n\r\n\r{\"animal_age\": \"${"
            + VAR_NAME + "}\"}\n\r\n\rThe body contains variable reference '" + VAR_NAME
            + "'.\n\rYou can define value for '" + VAR_NAME + "' in '" + GROOVY_SCRIPTING_TAB_NAME
            + "' section.\n\rEither in '" + BEFORE_FIRST_MSGS_SCRIPT_TAB_NAME + "' or '"
            + BEFORE_EACH_MSGS_SCRIPT_TAB + "' tab\n\r\n\rExample:\n\r1. Set " + REPEAT_COUNT_LABEL_TEXT
            + " to 3\n\r2. In '" + BEFORE_FIRST_MSGS_SCRIPT_TAB_NAME + "' tab write:\n\r    cat_age = 10;\n\r3. In '"
            + BEFORE_EACH_MSGS_SCRIPT_TAB + "' tab write\n\r    cat_age++\n\r4. Click '" + SEND_BUTTON_TEXT
            + "' button\n\r5. Notice that each message has value for '" + VAR_NAME + "' incremented.";

    public static final String BEFORE_FIRST_MSG_TAB_TOOLTIP = "This script (in groovy) will be executed only once just"
            + " before sending first message.\n\rYou can setup/define variable/classes that will be used later during"
            + " sending message\n\re.g.\n\rmessage_id = 1\n\r\n\rYou can refer to this variable in message body by"
            + " typing ${message_id}.";

    public static final String BEFORE_EACH_MSG_TAB_TOOLTIP = "This script (in groovy) will be executed before sending"
            + " every message.\n\rYou can set/modify variables here that were previously devined in the '"
            + BEFORE_FIRST_MSGS_SCRIPT_TAB_NAME + "' tab\n\re.g.\n\rmessage_id++\n\r\n\rYou can refer to this variable"
            + " in message body by typing ${message_id}.";

    public static final String BEFORE_FIRST_MSG_SHARED_TAB_TOOLTIP =
            "This script is shared across all senders.\n\rYou can set some 'global' variables here that will be"
                    + " accessible from all scripts for all senders.\n\rScripts are executed in order:\n\r\n\r1. "
                    + BEFORE_FIRST_MSGS_SHARED_SCRIPT_TAB_NAME + "\n\r2. " + BEFORE_FIRST_MSGS_SCRIPT_TAB_NAME
                    + "\n\r3. " + BEFORE_EACH_MSGS_SCRIPT_TAB
                    + "\n\r\n\rEach next script can access variable created in previous one.";
}
