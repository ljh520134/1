package moe.ouom.wekit.dexkit;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public class RedPacketDexResolver {

    public static Map<String, String> resolve(String apkPath, ClassLoader classLoader) throws Exception {
        Map<String, String> out = new HashMap<>();

        try (DexKitBridge bridge = DexKitBridge.create(apkPath)) {

            // ReceiveLuckyMoney ctor
            MethodMatcher mmReceive = new MethodMatcher();
            mmReceive.name("<init>");
            mmReceive.usingStrings("MicroMsg.NetSceneReceiveLuckyMoney");

            MethodData receiveCtor = bridge.findMethod(
                    FindMethod.create().matcher(mmReceive)
            ).single();

            // OpenLuckyMoney ctor
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

            // onGYNetEnd
            MethodMatcher mmOnGy = new MethodMatcher();
            mmOnGy.declaredClass(receiveClsDesc);
            mmOnGy.name("onGYNetEnd");
            mmOnGy.paramCount(3);

            MethodData onGy = bridge.findMethod(
                    FindMethod.create().matcher(mmOnGy)
            ).single();

            // queue class
            ClassMatcher cmQueue = new ClassMatcher();
            cmQueue.methods(ms -> ms.add(m -> {
                m.paramCount(4);
                m.usingStrings("MicroMsg.Mvvm.NetSceneObserverOwner");
            }));

            ClassData queueClass = bridge.findClass(
                    FindClass.create().matcher(cmQueue)
            ).single();

            // queue getter
            MethodMatcher mmGetter = new MethodMatcher();
            mmGetter.modifiers(Modifier.STATIC);
            mmGetter.paramCount(0);
            mmGetter.returnType(queueClass.getName());

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