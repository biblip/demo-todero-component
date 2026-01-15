package com.example.todero.agent.juno;

import com.social100.processor.AIAController;
import com.social100.processor.Action;
import com.social100.todero.common.ai.action.CommandAction;
import com.social100.todero.common.ai.agent.Agent;
import com.social100.todero.common.ai.agent.AgentContext;
import com.social100.todero.common.ai.agent.AgentDefinition;
import com.social100.todero.common.ai.agent.AgentPrompt;
import com.social100.todero.common.ai.llm.LLMClient;
import com.social100.todero.common.ai.llm.OllamaLLM;
import com.social100.todero.common.aiatpio.AiatpIO;
import com.social100.todero.common.command.CommandContext;
import com.social100.todero.common.config.ServerType;
import com.social100.todero.common.lineparser.LineParserUtil;
import com.social100.todero.common.storage.Storage;
import com.social100.todero.processor.EventDefinition;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@AIAController(name = "com.shellaia.verbatim.agent.dj",
    type = ServerType.AI,
    visible = true,
    description = "Simple Agent Demo",
    events = AgentComponentDemo.SimpleEvent.class)
public class AgentComponentDemo {
  final static String MAIN_GROUP = "Main";
  final AgentDefinition agentDefinition;
  final String openApiKey;
  private CommandContext globalContext = null;

  public AgentComponentDemo(Storage storage) {
    agentDefinition = AgentDefinition.builder()
        .name("DJ Agent")
        .role("Assistant")
        .description("handle a music playback system")
        //.model("gpt-4.1-nano")
        //.model("qwen3:4b")
        .model("gemma3:4b")
        //.model("gemma3:12b")
        .systemPrompt(AgentDefinition.loadSystemPromptFromResource("prompts/default-system-prompt.md"))
        .build();

    agentDefinition.setMetadata("region", "US");

    //Dotenv dotenv = Dotenv.configure().filename(".env").load();
    //this.openApiKey = dotenv.get("OPENAI_API_KEY","openai_api_key_value");
    this.openApiKey = "openai_api_key_value";
  }

  @Action(group = MAIN_GROUP,
      command = "process",
      description = "Send a prompt to the agent to process it")
  public Boolean agentProcess(CommandContext context) {
    AiatpIO.HttpRequest httpRequest = context.getHttpRequest();
    final String prompt = AiatpIO.bodyToString(httpRequest.body(), StandardCharsets.UTF_8);

    AgentContext agentContext = new AgentContext();

    // Agent 1: Planner (e.g. decomposes task)
    LLMClient llm = new OllamaLLM("http://127.0.0.1:11434", agentDefinition.getModel());
    //LLMClient llm = new OllamaAiLLM(this.openApiKey, agentDefinition.getModel());

    Agent planner = new Agent(agentDefinition);

    AgentPrompt agentPrompt = new AgentPrompt(prompt);

    try {
      CommandAction ss = (CommandAction) planner.process(llm, agentPrompt, agentContext);
      Optional<String> action = ss.getCommand();
      action.ifPresent(line -> {
        LineParserUtil.ParsedLine parsedLine = LineParserUtil.parse(line);
        String command = "ping"; // parsedLine.first;
        String newRemaining = parsedLine.second + (parsedLine.remaining == null ? "" : " " + parsedLine.remaining);

        CommandContext internalContext = CommandContext.builder()
            .httpRequest(AiatpIO.HttpRequest.newBuilder("ACTION","/com.example.todero.component.template/" + command)
                .body(AiatpIO.Body.ofString(newRemaining, StandardCharsets.UTF_8))
                .build())
            .consumer(context::response)
            .build();

        context.execute("com.example.todero.component.template", command, internalContext);
        //context.execute("vlc", command, internalContext);
      });

    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return true;
  }

  public enum SimpleEvent implements EventDefinition {
    SIMPLE_EVENT("A event to demo"),
    OTHER_EVENT("Other event to demo");

    private final String description;

    SimpleEvent(String description) {
      this.description = description;
    }

    @Override
    public String getDescription() {
      return description;
    }
  }
}
