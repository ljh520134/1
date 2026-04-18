package moe.ouom.wekit.dexkit;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public class RedPacketDexResolver {

    public static Map<String, String> resolve(String apkPath, ClassLoader classLoader) throws Exception {
        Map<String, String> out = new HashMap<>();

        try (DexKitBridge bridge = DexKitBridge.create(apkPath)) {

            // 1) ReceiveLuckyMoney ctor
            MethodMatcher mmReceive = new MethodMatcher();
            mmReceive.name("<init>");
            mmReceive.usingStrings("MicroMsg.NetSceneReceiveLuckyMoney");
            MethodData receiveCtor = bridge.findMethod(
                    FindMethod.create().matcher(mmReceive)
            ).single();

            // 2) OpenLuckyMoney ctor
            MethodMatcher mmOpen = new MethodMatcher();
            mmOpen.name("<init>");
            mmOpen.usingStrings("MicroMsg.NetSceneOpenLuckyMoney");
            MethodData openCtor = bridge.findMethod(
                    FindMethod.create().matcher(mmOpen)
            ).single();

            String receiveCtorDesc = receiveCtor.getDescriptor();
            String openCtorDesc = openCtor.getDescriptor();
            String receiveClsDesc = receiveCtorDesc.substring(0, receiveCtorDesc.indexOf("->"));
            String openClsDesc = openCtorDesc.substring(0, openCtorDesc.indexOf("->"));

            // 3) onGYNetEnd in receive class
            MethodMatcher mmOnGy = new MethodMatcher();
            mmOnGy.declaredClass(receiveClsDesc);
            mmOnGy.name("onGYNetEnd");
            mmOnGy.paramCount(3);
            MethodData onGy = bridge.findMethod(
                    FindMethod.create().matcher(mmOnGy)
            ).single();

            // 4) 先找任意 static/no-arg 方法 + 包含 "MicroMsg.Mvvm.NetSceneObserverOwner"
            //    再从结果里选 returnType 一样的方法作为 queue_getter
            MethodMatcher mmAnyStatic = new MethodMatcher();
            mmAnyStatic.modifiers(Modifier.STATIC);
            mmAnyStatic.paramCount(0);
            mmAnyStatic.usingStrings("MicroMsg.Mvvm.NetSceneObserverOwner");

            MethodData anyStatic = bridge.findMethod(
                    FindMethod.create().matcher(mmAnyStatic)
            ).single();

            String queueType = anyStatic.getMethodInstance(classLoader).getReturnType().getName();

            // 5) queue getter: static + no-arg + return queueType
            MethodMatcher mmGetter = new MethodMatcher();
            mmGetter.modifiers(Modifier.STATIC);
            mmGetter.paramCount(0);
            mmGetter.returnType(queueType);

            MethodData queueGetter = bridge.findMethod(
                    FindMethod.create().matcher(mmGetter)
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