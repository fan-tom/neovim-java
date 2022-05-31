/*
 * MIT License
 *
 * Copyright (c) 2022 Ensar Sarajčić
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.ensarsarajcic.neovim.java.pluginhost;

import com.ensarsarajcic.neovim.java.api.util.ObjectMappers;
import com.ensarsarajcic.neovim.java.corerpc.client.RpcClient;
import com.ensarsarajcic.neovim.java.corerpc.message.NotificationMessage;
import com.ensarsarajcic.neovim.java.corerpc.message.RequestMessage;
import com.ensarsarajcic.neovim.java.corerpc.message.ResponseMessage;
import com.ensarsarajcic.neovim.java.handler.NeovimHandlerManager;
import com.ensarsarajcic.neovim.java.handler.NeovimHandlerProxy;
import com.ensarsarajcic.neovim.java.handler.errors.NeovimHandlerException;
import com.ensarsarajcic.neovim.java.handler.util.ReflectionUtils;
import com.ensarsarajcic.neovim.java.pluginhost.annotations.NeovimAutocommand;
import com.ensarsarajcic.neovim.java.pluginhost.annotations.NeovimCommand;
import com.ensarsarajcic.neovim.java.pluginhost.annotations.NeovimJavaHostedPlugin;
import com.ensarsarajcic.neovim.java.pluginhost.opts.AutocommandOpts;
import com.ensarsarajcic.neovim.java.pluginhost.opts.CommandOpts;
import com.ensarsarajcic.neovim.java.pluginhost.state.AutocommandState;
import com.ensarsarajcic.neovim.java.pluginhost.state.CommandState;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class RemotePluginManager {

    private final NeovimHandlerManager neovimHandlerManager;
    private final NeovimHandlerProxy neovimHandlerProxy;
    private final RpcClient client;
    private final List<Runnable> initializers = new ArrayList<>();

    public RemotePluginManager(NeovimHandlerManager neovimHandlerManager, NeovimHandlerProxy neovimHandlerProxy, RpcClient rpcClient) {
        this.neovimHandlerManager = neovimHandlerManager;
        this.neovimHandlerProxy = neovimHandlerProxy;
        this.client = rpcClient;
    }

    public CompletableFuture<Void> setupRemotePlugins(NeovimJavaPluginHost pluginHost) {
        var reflections = new Reflections(
                new ConfigurationBuilder()
                        .addUrls(ClasspathHelper.forJavaClassPath())
                        .addScanners(Scanners.TypesAnnotated)
        );
        var hostedPlugins = reflections.getTypesAnnotatedWith(NeovimJavaHostedPlugin.class);
        var futures = new ArrayList<CompletableFuture<Void>>();
        for (var plugin : hostedPlugins) {
            Object instance = null;
            try {
                try {
                    var constructor = plugin.getDeclaredConstructor(NeovimJavaPluginHost.class);
                    // First create an instance and add it to handlers
                    instance = constructor.newInstance(pluginHost);
                } catch (NoSuchMethodException ex) {
                    // If there is no instance with correct param, take the default one
                    var constructor = plugin.getDeclaredConstructor();
                    instance = constructor.newInstance();
                }

                neovimHandlerManager.registerNeovimHandler(instance);
                try {
                    var initializerMethod = plugin.getDeclaredMethod("initialize");
                    Object finalInstance = instance;
                    initializers.add(() -> {
                        try {
                            initializerMethod.invoke(finalInstance);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    });
                } catch (NoSuchMethodException ignored) {
                }
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            futures.add(setupCommands(plugin, instance, pluginHost));
            futures.add(setupAutocommands(plugin, instance, pluginHost));
            pluginHost.getNeovimHandlerManager().registerNeovimHandler(instance);
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    public void startRemotePlugins() {
        initializers.forEach(Runnable::run);
    }

    private CompletableFuture<Void> setupCommands(Class<?> plugin, Object pluginInstance, NeovimJavaPluginHost pluginHost) {
        var commands = ReflectionUtils.getMethodsAnnotatedWith(plugin, NeovimCommand.class);
        var futures = new ArrayList<CompletableFuture<Void>>();
        for (var methodCommandEntry : commands) {
            NeovimCommand annotation = methodCommandEntry.getValue();
            String commandName = annotation.value();
            String handlerName = String.format("neovim-java-hosted-plugin:%s:%s", plugin.getCanonicalName(), commandName);
            if (annotation.sync()) {
                addRequestHandler(pluginInstance, handlerName, methodCommandEntry.getKey(), false);
                futures.add(pluginHost.getPluginApi().addRequestCommand(
                        commandName,
                        handlerName,
                        CommandOpts.fromAnnotation(annotation)
                ));
            } else {
                addNotificationHandler(pluginInstance, handlerName, methodCommandEntry.getKey(), false);
                futures.add(pluginHost.getPluginApi().addNotificationCommand(
                        commandName,
                        handlerName,
                        CommandOpts.fromAnnotation(annotation)
                ));
            }
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    private CompletableFuture<Void> setupAutocommands(Class<?> plugin, Object pluginInstance, NeovimJavaPluginHost pluginHost) {
        var autocommands = ReflectionUtils.getMethodsAnnotatedWith(plugin, NeovimAutocommand.class);
        var futures = new ArrayList<CompletableFuture<Integer>>();

        for (var methodAutocommandEntry : autocommands) {
            NeovimAutocommand annotation = methodAutocommandEntry.getValue();
            String handlerName = String.format("neovim-java-hosted-plugin:autocommand-handler:%s:%s", plugin.getCanonicalName(), UUID.randomUUID());
            if (annotation.sync()) {
                addRequestHandler(pluginInstance, handlerName, methodAutocommandEntry.getKey(), true);
            } else {
                addNotificationHandler(pluginInstance, handlerName, methodAutocommandEntry.getKey(), true);
            }
            futures.add(pluginHost.getPluginApi().addAutocommand(
                    Arrays.stream(annotation.value()).toList(),
                    handlerName,
                    AutocommandOpts.fromAnnotation(annotation)
            ));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    private void addRequestHandler(Object pluginInstance, String handlerName, Method method, boolean autocommand) {
        neovimHandlerProxy.addRequestCallback(requestMessage -> {
            if (handlerName.equals(requestMessage.getMethod())) {
                try {
                    var result = invokeCommandMethod(
                            pluginInstance,
                            method,
                            requestMessage,
                            RequestMessage.class,
                            autocommand
                    );
                    client.send(new ResponseMessage(requestMessage.getId(), null, result));
                } catch (Exception ex) {
                    if (ex.getCause() instanceof NeovimHandlerException) {
                        try {
                            client.send(new ResponseMessage(requestMessage.getId(), ((NeovimHandlerException) ex.getCause()).toRpcError(), null));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        ex.printStackTrace();
                    }
                }
            }
        });
    }

    private void addNotificationHandler(Object pluginInstance, String handlerName, Method method, boolean autocommand) {
        neovimHandlerProxy.addNotificationCallback(notificationMessage -> {
            if (handlerName.equals(notificationMessage.getName())) {
                try {
                    invokeCommandMethod(
                            pluginInstance,
                            method,
                            notificationMessage,
                            NotificationMessage.class,
                            autocommand
                    );
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private Object invokeCommandMethod(
            Object pluginInstance,
            Method method,
            Object message,
            Class<?> messageClass,
            boolean autocommand
    ) throws Exception {
        Object result = null;
        NeovimHandlerException error = null;
        Object targetObject = null;
        if (!Modifier.isStatic(method.getModifiers())) {
            targetObject = pluginInstance;
        }

        if (method.getParameterCount() > 1) {
            throw new RuntimeException("Command and autocommand methods can only take a single argument!");
        } else if (method.getParameterCount() > 0) {
            Object param = null;
            Class<?> paramType = method.getParameterTypes()[0];
            if (paramType == messageClass) {
                param = message;
            } else if (!autocommand && paramType == CommandState.class) {
                Object data;
                if (message instanceof RequestMessage) {
                    data = ((RequestMessage) message).getArguments().get(0);
                } else if (message instanceof NotificationMessage) {
                    data = ((NotificationMessage) message).getArguments().get(0);
                } else {
                    throw new RuntimeException("Unsupported message type: " + message);
                }
                param = ObjectMappers.defaultNeovimMapper().readerFor(CommandState.class).readValue(
                        ObjectMappers.defaultNeovimMapper().writeValueAsBytes(data)
                );
            } else if (autocommand && paramType == AutocommandState.class) {
                Object data;
                if (message instanceof RequestMessage) {
                    data = ((RequestMessage) message).getArguments().get(0);
                } else if (message instanceof NotificationMessage) {
                    data = ((NotificationMessage) message).getArguments().get(0);
                } else {
                    throw new RuntimeException("Unsupported message type: " + message);
                }
                param = ObjectMappers.defaultNeovimMapper().readerFor(AutocommandState.class).readValue(
                        ObjectMappers.defaultNeovimMapper().writeValueAsBytes(data)
                );
            } else {
                if (autocommand) {
                    throw new RuntimeException("Autocommand methods can only take a RequestMessage or AutocommandState!");
                } else {
                    throw new RuntimeException("Command methods can only take a RequestMessage or CommandState!");
                }
            }

            result = method.invoke(targetObject, param);
        } else {
            result = method.invoke(targetObject);
        }
        return result;
    }
}
