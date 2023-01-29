package org.embeddedt.blacksmith.impl;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.IncompatibleEnvironmentException;
import net.bytebuddy.agent.ByteBuddyAgent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class AgentInjectionService implements ITransformationService {
    private static final Logger LOGGER = LogManager.getLogger("Blacksmith");

    private static String getJavaAgentArg() {
        Path jarPath;
        try {
            jarPath = new File(AgentInjectionService.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toPath();
            jarPath = jarPath.getParent().getParent().relativize(jarPath);
        } catch(URISyntaxException | NullPointerException e) {
            e.printStackTrace();
            System.exit(1);
            return null;
        }
        return "-javaagent:" + jarPath;
    }

    /**
     * Try to inject ourselves as an agent if possible. If not, print an error.
     */
    public AgentInjectionService() {

        if(Agent.instrumentation == null) {
            String arg = getJavaAgentArg();
            LOGGER.error("[Blacksmith] trying to inject fallback agent, please consider adding " + arg + " to your JVM arguments");
            Instrumentation instrumentation;
            try {
                instrumentation = ByteBuddyAgent.install();
            } catch(IllegalStateException e) {
                LOGGER.error("[Blacksmith] Agent injection failed. Please add " + arg + " to your JVM arguments", e);
                System.exit(1);
                return; /* not reached */
            }
            LOGGER.info("Successfully injected fallback agent");
            Agent.fallbackNeeded = true;
            Agent.premain(null, instrumentation);
        } else {
            LOGGER.info("Agent already loaded via normal mechanism");
        }
    }
    @Override
    public String name() {
        return "blacksmith";
    }

    @Override
    public void initialize(IEnvironment environment) {

    }

    @Override
    public void beginScanning(IEnvironment environment) {

    }

    @Override
    public void onLoad(IEnvironment env, Set<String> otherServices) throws IncompatibleEnvironmentException {

    }

    @Override
    public List<ITransformer> transformers() {
        return Collections.emptyList();
    }
}
