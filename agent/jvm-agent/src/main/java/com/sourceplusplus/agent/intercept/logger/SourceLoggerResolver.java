package com.sourceplusplus.agent.intercept.logger;

import org.apache.skywalking.apm.agent.core.boot.AgentPackageNotFoundException;
import org.apache.skywalking.apm.agent.core.boot.AgentPackagePath;
import org.apache.skywalking.apm.agent.core.conf.Config;
import org.apache.skywalking.apm.agent.core.conf.Constants;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.core.EasyLogResolver;
import org.apache.skywalking.apm.agent.core.logging.core.EasyLogger;
import org.apache.skywalking.apm.agent.core.logging.core.FileWriter;
import org.apache.skywalking.apm.agent.core.logging.core.LogLevel;
import org.apache.skywalking.apm.util.StringUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * todo: description
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.1.2
 * @since 0.1.0
 */
public class SourceLoggerResolver extends EasyLogResolver {

    @Override
    public ILog getLogger(Class<?> clazz) {
        return new EasyLogger(clazz) {
            @Override
            protected void logger(LogLevel level, String message, Throwable e) {
                if (StringUtil.isEmpty(Config.Logging.DIR)) {
                    try {
                        Config.Logging.DIR = findPath() + "/logs";
                    } catch (AgentPackageNotFoundException ex) {
                        ex.printStackTrace();
                    }
                }
                FileWriter.get().write(format(level, message, e));
            }

            String format(LogLevel level, String message, Throwable t) {
                return StringUtil.join(' ', level.name(),
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(new Date()),
                        clazz.getSimpleName(),
                        ": ",
                        message,
                        t == null ? "" : format(t)
                );
            }

            String format(Throwable t) {
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                t.printStackTrace(new java.io.PrintWriter(buf, true));
                String expMessage = buf.toString();
                try {
                    buf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return Constants.LINE_SEPARATOR + expMessage;
            }
        };
    }

    private static File findPath() throws AgentPackageNotFoundException {
        String classResourcePath = AgentPackagePath.class.getName().replaceAll("\\.", "/") + ".class";

        URL resource = AgentPackagePath.class.getClassLoader().getSystemClassLoader().getResource(classResourcePath);
        if (resource != null) {
            String urlString = resource.toString();

            //logger.debug("The beacon class location is {}.", urlString);

            int insidePathIndex = urlString.indexOf('!');
            boolean isInJar = insidePathIndex > -1;

            if (isInJar) {
                urlString = urlString.substring(urlString.indexOf("file:"), insidePathIndex);
                File agentJarFile = null;
                try {
                    agentJarFile = new File(new URL(urlString).getFile());
                } catch (MalformedURLException e) {
                    //logger.error(e, "Can not locate agent jar file by url:" + urlString);
                }
                if (agentJarFile.exists()) {
                    return agentJarFile.getParentFile();
                }
            } else {
                String classLocation = urlString.substring(urlString.indexOf("file:"), urlString.length() - classResourcePath.length());
                return new File(classLocation);
            }
        }

        //logger.error("Can not locate agent jar file.");
        throw new AgentPackageNotFoundException("Can not locate agent jar file.");
    }
}
