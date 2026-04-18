package moe.ouom.wekit.dexkit;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public class RedPacketDexResolver {

    public static Map<String, String> resolve(String apkPath, ClassLoader classLoader) throws Exception {
        Map<String, String> out = new HashMap<>();

        try (DexKitBridge bridge = DexKitBridge.create(apkPath)) {
            // 1) ReceiveLuckyMoney 构造
            MethodData receiveCtor = bridge.findMethod(
                    FindMethod.create().matcher(m -> m
                            .name("<init>")
                            .usingStrings("MicroMsg.NetSceneReceiveLuckyMoney"))
            ).single();

            // 2) OpenLuckyMoney 构造
            MethodData openCtor = bridge.findMethod(
                    FindMethod.create().matcher(m -> m
                            .name("<init>")
                            .usingStrings("MicroMsg.NetSceneOpenLuckyMoney"))
            ).single();

            String receiveCtorDesc = receiveCtor.getDescriptor();
            String openCtorDesc = openCtor.getDescriptor();

            String receiveClsDesc = receiveCtorDesc.substring(0, receiveCtorDesc.indexOf("->"));
            String openClsDesc = openCtorDesc.substring(0, openCtorDesc.indexOf("->"));

            // 3) onGYNetEnd
            MethodData onGy = bridge.findMethod(
                    FindMethod.create().matcher(m -> m
                            .declaredClass(receiveClsDesc)
                            .name("onGYNetEnd")
                            .paramCount(3))
            ).single();

            // 4) NetSceneQueue 类
            ClassData queueClass = bridge.findClass(
                    FindClass.create().matcher(c -> c.methods(ms -> ms.add(mm -> mm
                            .paramCount(4)
                            .usingStrings("MicroMsg.Mvvm.NetSceneObserverOwner"))))
            ).single();

            // 5) queue getter
            MethodData queueGetter = bridge.findMethod(
                    FindMethod.create().matcher(m -> m
                            .modifiers(Modifier.STATIC)
                            .paramCount(0)
                            .returnType(queueClass.getName()))
            ).single();

            out.put("receive_cls", receiveClsDesc);
            out.put("open_cls", openClsDesc);
            out.put("receive_ctor", receiveCtorDesc);
            out.put("open_ctor", openCtorDesc);
            out.put("on_gynetend", onGy.getDescriptor());
            out.put("queue_getter", queueGetter.getDescriptor());
        }

        return out;
    }
}