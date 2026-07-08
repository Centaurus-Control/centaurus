package de.shadowsoft.centaurus.agent.script;

import de.shadowsoft.centaurus.agent.config.AgentScriptConfig;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent/scripts")
public class AgentScriptConfigController {

    private final AgentScriptConfigService scriptConfigService;

    public AgentScriptConfigController(AgentScriptConfigService scriptConfigService) {
        this.scriptConfigService = scriptConfigService;
    }

    @GetMapping
    public List<AgentScriptConfig> listScripts() {
        return scriptConfigService.listScripts();
    }

    @PostMapping
    public AgentScriptConfig saveScript(@RequestBody AgentScriptConfig script) {
        return scriptConfigService.saveScript(script);
    }

    @DeleteMapping("/{scriptId}")
    public void deleteScript(@PathVariable UUID scriptId) {
        scriptConfigService.deleteScript(scriptId);
    }

    @PostMapping("/publish-manifest")
    public Map<String, Object> publishManifest() {
        scriptConfigService.publishManifest();
        return Map.of("published", true);
    }
}
