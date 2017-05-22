package io.invoker.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.invoker.InvokerCommand;
import io.invoker.flow.FlowController;
import io.netty.channel.ChannelHandlerContext;
import io.remoting.netty.NettyCommandProcessor;
import io.remoting.protocol.ProtocolFactory;
import io.remoting.protocol.ProtocolFactorySelector;
import io.remoting.protocol.RemotingCommand;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月22日 下午4:09:36
 */
public class InvokerCommandProcessor implements NettyCommandProcessor {
    private static final Logger log = LoggerFactory.getLogger(InvokerCommandProcessor.class.getSimpleName());
    private ProtocolFactorySelector protocolFactorySelector;
    private ServiceObjectFinder serviceObjectFinder;
    private FlowController flowController;

    @Override
    public RemotingCommand processCommand(ChannelHandlerContext ctx, RemotingCommand request) throws Throwable {
        ProtocolFactory protocolFactory = protocolFactorySelector.select(request.getProtocolCode());
        InvokerCommand command = protocolFactory.decode(InvokerCommand.class, request);
        ServiceObject serviceObject = serviceObjectFinder.getServiceObject(command);
        try {
            Object retObject = serviceObject.invoke(command);
            command.setRetObject(retObject);
        } catch (Throwable e) {
            command.setT(e);
            log.error("invoke serviceId<" + command.commandSignature() + "> Error.", e);
        }
        RemotingCommand response = RemotingCommand.replyCommand(request, request.getCode());
        protocolFactory.encode(command, response);
        return response;
    }

    @Override
    public boolean reject() {
        if (flowController == null) {
            return false;
        }
        return flowController.reject();
    }
    
    public void setProtocolFactorySelector(ProtocolFactorySelector protocolFactorySelector) {
        this.protocolFactorySelector = protocolFactorySelector;
    }
    
    public void setServiceObjectFinder(ServiceObjectFinder serviceObjectFinder) {
        this.serviceObjectFinder = serviceObjectFinder;
    }
    
    public void setFlowController(FlowController flowController) {
        this.flowController = flowController;
    }
}
