package com.joedobo27.rahlm;


import javassist.*;
import javassist.Modifier;
import javassist.bytecode.Descriptor;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RemoveAnimalHoardingLimitsMod implements WurmServerMod, Initable {

    private static final Logger logger = Logger.getLogger(RemoveAnimalHoardingLimitsMod.class.getName());

    @Override
    public void init() {
        try {
            // Modify com.wurmonline.server.behaviours.MethodsCreatures.brand(Creature,Creature,Item,Action,float)
            // so the method doesn't block branding brand count exceeds citizen count.
            HookManager.getInstance().getClassPool().get("com.wurmonline.server.behaviours.MethodsCreatures")
                    .getDeclaredMethod("brand", new CtClass[]{
                            HookManager.getInstance().getClassPool().get("com.wurmonline.server.creatures.Creature"),
                            HookManager.getInstance().getClassPool().get("com.wurmonline.server.creatures.Creature"),
                            HookManager.getInstance().getClassPool().get("com.wurmonline.server.items.Item"),
                            HookManager.getInstance().getClassPool().get("com.wurmonline.server.behaviours.Action"),
                            CtPrimitiveType.floatType
                    }).instrument(new ExprEditor() {
                        @Override public void edit(MethodCall methodCall) throws CannotCompileException {
                            if (Objects.equals("getMaxCitizens", methodCall.getMethodName())){
                                methodCall.replace("$_ = Integer.MAX_VALUE;");
                                logger.log(Level.FINE, "set branding count limit to Integer.MAX_VALUE " +
                                        "at " + methodCall.getLineNumber());
                            }
                        }
            });


            // Modify com.wurmonline.server.creatures.Creature.checkPregnancy(boolean) so it always uses a deed ratio
            // of 100.
            HookManager.getInstance().getClassPool().get("com.wurmonline.server.creatures.Creature")
                    .getMethod("checkPregnancy", Descriptor.ofMethod(CtPrimitiveType.booleanType, new CtClass[]{
                            CtPrimitiveType.booleanType
                    })).instrument(new ExprEditor() {
                        @Override public void edit(MethodCall methodCall) throws CannotCompileException {
                            if (Objects.equals("getCreatureRatio", methodCall.getMethodName())) {
                                methodCall.replace("$_ = 100.0f;");
                                logger.log(Level.FINE, "set creature ratio in Creature.class checkPregnancy()" +
                                        " to always be 100.0f at " + methodCall.getLineNumber());
                            }
                        }
            });


            // Modify com.wurmonline.server.creatures.CreatureStatus.checkDisease() so it always uses a deed ratio
            // of 100.
            HookManager.getInstance().getClassPool().get("com.wurmonline.server.creatures.CreatureStatus")
                    .getDeclaredMethod("checkDisease", null).instrument(new ExprEditor() {
                @Override public void edit(MethodCall methodCall) throws CannotCompileException {
                    if (Objects.equals("getCreatureRatio", methodCall.getMethodName())) {
                        methodCall.replace("$_ = 100.0f;");
                        logger.log(Level.FINE, "set creature ratio in creatures.class checkDisease()" +
                                " to always be 100.0f at " + methodCall.getLineNumber());
                    }
                }
            });


            // Modify the com.wurmonline.server.questions.VillageInfo.sendQuestion so it displays a ratio of 100.
            HookManager.getInstance().getClassPool().get("com.wurmonline.server.questions.VillageInfo")
                    .getMethod("sendQuestion","()V").instrument(new ExprEditor() {
                        @Override public void edit(MethodCall methodCall) throws CannotCompileException {
                            if (Objects.equals("getCreatureRatio", methodCall.getMethodName())){
                                methodCall.replace("$_ = 100.0f;");
                                logger.log(Level.FINE, "set creature ratio in VillageInfo.class sendQuestion()" +
                                        " to always be 100.0f at " + methodCall.getLineNumber());
                            }
                        }
            });


            //Modify the com.wurmonline.server.zones.VolaTile.checkDiseaseSpread() so disease can't spread.
            //void the entire method as it doesn't do anything else.
            HookManager.getInstance().getClassPool().get("com.wurmonline.server.zones.VolaTile")
                    .getMethod("checkDiseaseSpread", "()V").setBody("return;");


            //Modify com.wurmonline.server.creatures.CreatureStatus.checkDisease() so it uses the empty creatures
            // array ( in VolaTile.class) instead of whatever is in proximity. Also setting emptyCreatures array to public.
            CtField emptyCreatures = HookManager.getInstance().getClassPool().get("com.wurmonline.server.zones.VolaTile")
                    .getDeclaredField("emptyCreatures", "[Lcom/wurmonline/server/creatures/Creature;");
            emptyCreatures.setModifiers(Modifier.setPublic(emptyCreatures.getModifiers()));

            HookManager.getInstance().getClassPool().get("com.wurmonline.server.creatures.CreatureStatus")
                    .getDeclaredMethod("checkDisease", null)
                    .instrument(new ExprEditor() {
                        public void edit(MethodCall methodCall) throws CannotCompileException {
                            if (Objects.equals("getCreatures", methodCall.getMethodName())){
                                methodCall.replace("$_ = com.wurmonline.server.zones.VolaTile.emptyCreatures;");
                                logger.log(Level.FINE, "Set checkDisease() of CreatureStatus.class to use" +
                                        " VolaTile.emptyCreatures field instead of the proximity creature count" +
                                        " at " + methodCall.getLineNumber());
                            }
                        }
                    });

        } catch (NotFoundException | CannotCompileException e) {
            logger.log(Level.WARNING, e.toString(), e);
        }
    }
}
