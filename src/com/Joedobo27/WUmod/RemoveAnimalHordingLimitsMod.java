package com.Joedobo27.WUmod;


import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.Configurable;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;

import java.util.Objects;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RemoveAnimalHordingLimitsMod implements WurmServerMod, Initable, Configurable {

    private boolean removeBrandLimit = true;
    private boolean removeCreatureRatioAffects = true;
    private boolean removeDiseaseSpread = true;
    private static ClassPool pool;

    private static final Logger logger = Logger.getLogger(RemoveAnimalHordingLimitsMod.class.getName());

    @Override
    public void configure(Properties properties) {
        removeBrandLimit = Boolean.parseBoolean(properties.getProperty("removeBrandLimit", Boolean.toString(removeBrandLimit)));
        removeCreatureRatioAffects = Boolean.parseBoolean(properties.getProperty("removeCreatureRationAffects", Boolean.toString(removeCreatureRatioAffects)));
        removeDiseaseSpread = Boolean.parseBoolean(properties.getProperty("removeDiseaseSpread", Boolean.toString(removeDiseaseSpread)));
    }

    @Override
    public void init() {
        pool = HookManager.getInstance().getClassPool();
        try {
            removeBrandLimitBytecode();
            removeCreatureRationAffectsBytecode();
            removeDiseaseSpreadBytecode();
        } catch (NotFoundException | CannotCompileException e) {
            logger.log(Level.WARNING, e.toString(), e);
        }
    }

    private void removeBrandLimitBytecode() throws NotFoundException, CannotCompileException {
        if (!removeBrandLimit)
            return;
        CtMethod cmBrand = pool.get("com.wurmonline.server.behaviours.MethodsCreatures").getMethod("brand",
                "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/behaviours/Action;F)Z");
        cmBrand.instrument(new ExprEditor() {
            @Override
            public void edit(MethodCall methodCall) throws CannotCompileException {
                if (Objects.equals("getMaxCitizens", methodCall.getMethodName()) && methodCall.getLineNumber() == 2444){
                    methodCall.replace("$_ = Integer.MAX_VALUE;");
                    logger.log(Level.INFO, "set branding count limit to Integer.MAX_VALUE at " + methodCall.getLineNumber());
                }
            }
        });
    }

    private void removeCreatureRationAffectsBytecode() throws NotFoundException, CannotCompileException{
        if (!removeCreatureRatioAffects)
            return;
        CtMethod cmCheckPregnancy = pool.get("com.wurmonline.server.creatures.Creature").getMethod("checkPregnancy", "(Z)Z");
        cmCheckPregnancy.instrument(new ExprEditor() {
            @Override
            public void edit(MethodCall methodCall) throws CannotCompileException {
                if (Objects.equals("getCreatureRatio", methodCall.getMethodName())) {
                    methodCall.replace("$_ = 100.0f;");
                    logger.log(Level.INFO, "set creature ratio in Creature.class checkPregnancy() to always be 100.0f at " + methodCall.getLineNumber());
                }
            }
        });

        CtMethod cmCheckDisease = pool.get("com.wurmonline.server.creatures.CreatureStatus").getDeclaredMethod("checkDisease");
        cmCheckDisease.instrument(new ExprEditor() {
            @Override
            public void edit(MethodCall methodCall) throws CannotCompileException {
                if (Objects.equals("getCreatureRatio", methodCall.getMethodName())){
                    methodCall.replace("$_ = 100.0f;");
                    logger.log(Level.INFO, "set creature ratio in CreatureStatus.class checkDisease() to always be 100.0f at " + methodCall.getLineNumber());
                }
            }
        });

        CtMethod cmSendQuestion = pool.get("com.wurmonline.server.questions.VillageInfo").getMethod("sendQuestion","()V");
        cmSendQuestion.instrument(new ExprEditor() {
            @Override
            public void edit(MethodCall methodCall) throws CannotCompileException {
                if (Objects.equals("getCreatureRatio", methodCall.getMethodName())){
                    methodCall.replace("$_ = 100.0f;");
                    logger.log(Level.INFO, "set creature ratio in VillageInfo.class sendQuestion() to always be 100.0f at " + methodCall.getLineNumber());
                }
            }
        });

        CtMethod cmCheckDiseaseSpread = pool.get("com.wurmonline.server.zones.VolaTile").getMethod("checkDiseaseSpread", "()V");
        cmCheckDiseaseSpread.instrument(new ExprEditor() {
            @Override
            public void edit(MethodCall methodCall) throws CannotCompileException {
                if (Objects.equals("getCreatureRatio", methodCall.getMethodName())){
                    methodCall.replace("$_ = 100.0f;");
                    logger.log(Level.INFO, "set creature ratio in VolaTile.class checkDiseaseSpread() to always be 100.0f at " + methodCall.getLineNumber());
                }
            }
        });
    }

    private void removeDiseaseSpreadBytecode() throws NotFoundException, CannotCompileException {
        if (!removeDiseaseSpread)
            return;
        CtMethod cmCheckDiseaseSpread = pool.get("com.wurmonline.server.zones.VolaTile").getMethod("checkDiseaseSpread", "()V");
        cmCheckDiseaseSpread.instrument(new ExprEditor() {
            @Override
            public void edit(MethodCall methodCall) throws CannotCompileException {
                if (Objects.equals("getThisAndSurroundingTiles", methodCall.getMethodName())){
                    methodCall.replace("{ $1=0; $_ = $proceed($$); }");
                    logger.log(Level.INFO, "set the disease spread search zone size to 0 in VolaTile.class checkDiseaseSpread() at " + methodCall.getLineNumber());
                }
            }
        });
    }

}
