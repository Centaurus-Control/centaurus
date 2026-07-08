package de.shadowsoft.centaurus.agent.files;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent/files")
public class AgentFileBrowserController {

    private final AgentFileBrowserService fileBrowserService;

    public AgentFileBrowserController(AgentFileBrowserService fileBrowserService) {
        this.fileBrowserService = fileBrowserService;
    }

    @GetMapping
    public AgentFileListResponse list(@RequestParam(required = false) String path) {
        return fileBrowserService.list(path);
    }
}
