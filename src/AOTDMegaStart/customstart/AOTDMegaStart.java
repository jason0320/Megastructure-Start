package AOTDMegaStart.customstart;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.Script;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.CharacterCreationData;
import com.fs.starfarer.api.impl.MusicPlayerPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.*;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.MiscellaneousThemeGenerator;
import com.fs.starfarer.api.impl.campaign.rulecmd.AoTDMegastructureRules;
import com.fs.starfarer.api.impl.campaign.rulecmd.newgame.Nex_NGCStartFleetOptionsV2;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.vok.campaign.econ.globalproduction.models.megastructures.GPBaseMegastructure;
import exerelin.campaign.ExerelinSetupData;
import exerelin.campaign.PlayerFactionStore;
import exerelin.campaign.customstart.CustomStart;
import exerelin.utilities.NexConfig;
import exerelin.utilities.NexFactionConfig;
import lunalib.lunaSettings.LunaSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AOTDMegaStart extends CustomStart {

    private static final String SYSTEM_ID = "new sol";
    private static final String STAR_ID = "new sol";
    private static final String PLAYER_PLANET_ID = "new terra";
    private static final String NID_ID = "new mars";
    private static final String PLUTO_ID = "new pluto";
    private static final String PLUTO_STATION_ID = "new pluto station";
    private static final String MINERVA_ID = "new minerva";
    private static final String GATE_ID = "new sol gate";
    private static final String BUOY_ID = "new sol buoy";
    private static final String RELAY_ID = "new sol relay";
    private static final String ARRAY_ID = "new sol array";

    public class RandomNameGenerator {

        // Random STAR name
        public static String getRandomStarName() {
            ProcgenUsedNames.NamePick pick = ProcgenUsedNames.pickName(
                    NameGenData.TAG_STAR,
                    null, // no parent
                    null  // no lagrange type
            );
            return pick.nameWithRomanSuffixIfAny;
        }

        // Random PLANET name (optionally linked to star/system name)
        public static String getRandomPlanetName(String parentName) {
            ProcgenUsedNames.NamePick pick = ProcgenUsedNames.pickName(
                    NameGenData.TAG_PLANET,
                    parentName, // makes names like "X IV"
                    null
            );
            return pick.nameWithRomanSuffixIfAny;
        }

        // Random MOON name
        public static String getRandomMoonName(String parentName) {
            ProcgenUsedNames.NamePick pick = ProcgenUsedNames.pickName(
                    NameGenData.TAG_MOON,
                    parentName,
                    null
            );
            return pick.nameWithRomanSuffixIfAny;
        }
    }

    @Override
    public void execute(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        CharacterCreationData data = (CharacterCreationData) memoryMap.get(MemKeys.LOCAL).get("$characterData");

        data.setDifficulty("normal");
        ExerelinSetupData.getInstance().easyMode = false;
        ExerelinSetupData.getInstance().freeStart = true;

        PlayerFactionStore.setPlayerFactionIdNGC(Factions.PLAYER);
        patchFactionStartPools(Factions.PLAYER);

        new Nex_NGCStartFleetOptionsV2().addOptions(dialog, memoryMap);

        data.addScriptBeforeTimePass(new Script() {
            @Override
            public void run() {
                SectorAPI sector = Global.getSector();
                CampaignFleetAPI fleet = sector.getPlayerFleet();
                SectorEntityToken home = sector.getEntityById(PLAYER_PLANET_ID);

                if (fleet != null && home != null) {
                    exerelin.campaign.StartSetupPostTimePass.sendPlayerFleetToLocation(fleet, home);
                }

                boolean nodupeMode = Boolean.TRUE.equals(LunaSettings.getBoolean("megastructure_start", "nodupeMode"));
                if (nodupeMode){
                    cleanupExtraAotdContent();
                }
                Global.getSector().getMemoryWithoutUpdate().set("$aotdPerfectStart", true);
                Global.getSector().getMemoryWithoutUpdate().set("$nex_startLocation", PLAYER_PLANET_ID);
                spawnRandomBlueGiantSystem();
            }
        });
    }

    private void spawnRandomBlueGiantSystem() {
        SectorAPI sector = Global.getSector();

        StarSystemAPI sys = sector.getStarSystem(SYSTEM_ID);
        if (sys == null) {
            sys = sector.createStarSystem(SYSTEM_ID);
        }

        sys.setProcgen(true);
        String systemName = "New Sol";
        sys.setName(systemName);
        boolean randomMode = Boolean.TRUE.equals(LunaSettings.getBoolean("megastructure_start", "randomMode"));

        float x;
        float y;
        if (randomMode) {
            x = (Misc.random.nextFloat() * 2f - 1f) * 40000f;
            y = (Misc.random.nextFloat() * 2f - 1f) * 40000f;
        } else {
            Float xSetting = LunaSettings.getFloat("megastructure_start", "customXpos");
            Float ySetting = LunaSettings.getFloat("megastructure_start", "customYpos");
            x = xSetting != null ? xSetting : -26000f;
            y = ySetting != null ? ySetting : 11000f;
        }

        sys.initStar(STAR_ID, StarTypes.BLUE_GIANT, 600f, x, y, 600f);
        sys.generateAnchorIfNeeded();
        sys.autogenerateHyperspaceJumpPoints(true, true);
        sys.addTag(Tags.HAS_CORONAL_TAP);
        boolean newsolMode = Boolean.TRUE.equals(LunaSettings.getBoolean("megastructure_start", "newsolMode"));
        String starName = "New Sol";
        if (newsolMode){
            starName = RandomNameGenerator.getRandomStarName();
            sys.getStar().setName(starName);
            sys.setBaseName(starName);
        }
        String parentName = sys.getName();

        PlanetAPI playerPlanet = sys.addPlanet(
                PLAYER_PLANET_ID,
                sys.getStar(),
                "New Terra",
                Planets.PLANET_TERRAN,
                randAngle(),
                randOrbitRadius(50f, 300f),
                randOrbitRadius(1800f, 3500f),
                randOrbitDays(1500f, 2600f)
        );
        if (newsolMode) {
            String playerPlanetName = RandomNameGenerator.getRandomPlanetName(starName);
            playerPlanet.setName(playerPlanetName);
        }

        MarketAPI playerMarket = createMarket(playerPlanet, "aotd_perfect_sys_colony", 5, Factions.PLAYER);
        playerMarket.addCondition(Conditions.HABITABLE);
        playerMarket.addCondition(Conditions.MILD_CLIMATE);
        playerMarket.addCondition(Conditions.FARMLAND_RICH);
        playerMarket.addCondition(Conditions.ORE_RICH);
        playerMarket.addCondition(Conditions.RARE_ORE_RICH);
        playerMarket.addCondition(Conditions.ORGANICS_PLENTIFUL);
        playerMarket.addCondition(Conditions.POPULATION_5);
        playerMarket.addIndustry(Industries.POPULATION);
        playerMarket.addIndustry(Industries.SPACEPORT);
        playerMarket.addIndustry(Industries.WAYSTATION);
        playerMarket.addIndustry(Industries.BATTLESTATION);
        playerMarket.addIndustry(Industries.FARMING);
        playerMarket.addIndustry(Industries.MINING);
        playerMarket.addIndustry(Industries.COMMERCE);
        playerMarket.addIndustry(Industries.PATROLHQ);
        playerMarket.addIndustry(Industries.GROUNDDEFENSES);
        playerMarket.addSubmarket(Submarkets.LOCAL_RESOURCES);
        playerMarket.addSubmarket(Submarkets.SUBMARKET_OPEN);
        playerMarket.addSubmarket(Submarkets.SUBMARKET_STORAGE);
        finalizePlayerMarket(playerMarket);

        CustomEntitySpecAPI spec = Global.getSettings().getCustomEntitySpec(Entities.CORONAL_TAP);
        BaseThemeGenerator.EntityLocation loc = new BaseThemeGenerator.EntityLocation();
        float orbitRadius = sys.getStar().getRadius() + spec.getDefaultRadius() + 100f;
        float orbitDays = orbitRadius / 20f;
        loc.orbit = Global.getFactory().createCircularOrbitPointingDown(
                sys.getStar(),
                randAngle(),
                orbitRadius,
                orbitDays
        );
        BaseThemeGenerator.addEntity(Misc.random, sys, loc, Entities.CORONAL_TAP, Factions.NEUTRAL);

        PlanetAPI nidavelir = sys.addPlanet(
                NID_ID,
                sys.getStar(),
                "New Mars",
                Planets.ARID,
                randAngle(),
                randOrbitRadius(50f, 300f),
                randOrbitRadius(2600f, 4200f),
                randOrbitDays(2200f, 3200f)
        );
        if (newsolMode) {
            String nidavelirName = RandomNameGenerator.getRandomPlanetName(starName);
            nidavelir.setName(nidavelirName);
        }
        MarketAPI nidMarket = nidavelir.getMarket();
        nidMarket.setFactionId(Factions.NEUTRAL);
        nidavelir.addTag(Tags.NOT_RANDOM_MISSION_TARGET);

        GPBaseMegastructure nidMega = AoTDMegastructureRules.putMegastructure(nidavelir, "aotd_nidavelir");
        String nidCond = nidMarket.addCondition("aotd_nidavelir_complex");
        nidMarket.getSpecificCondition(nidCond).setSurveyed(false);
        nidMarket.addCondition(Conditions.HABITABLE);
        nidMarket.addCondition(Conditions.FARMLAND_ADEQUATE);
        nidMarket.addCondition(Conditions.RUINS_VAST);
        nidMarket.addCondition(Conditions.VERY_HOT);
        nidMarket.addCondition(Conditions.RARE_ORE_ULTRARICH);
        nidMarket.addCondition(Conditions.ORE_ULTRARICH);
        nidMarket.setSurveyLevel(MarketAPI.SurveyLevel.NONE);

        if (nidMega != null && nidMega.getSpec() != null) {
            sector.getPlayerMemoryWithoutUpdate().set(
                    "$aotd_mega_system_id_" + nidMega.getSpec().getMegastructureID(),
                    nidavelir.getStarSystem().getId()
            );
        }

        PlanetAPI pluto = sys.addPlanet(
                PLUTO_ID,
                sys.getStar(),
                "New Pluto",
                Planets.ARID,
                randAngle(),
                randOrbitRadius(50f, 300f),
                randOrbitRadius(4200f, 6200f),
                randOrbitDays(3200f, 4800f)
        );
        if (newsolMode) {
            String plutoName = RandomNameGenerator.getRandomPlanetName(starName);
            pluto.setName(plutoName);
        }
        MarketAPI plutoMarket = pluto.getMarket();
        plutoMarket.setFactionId(Factions.NEUTRAL);
        String plutoCond = plutoMarket.addCondition("aotd_pluto_station");
        plutoMarket.getSpecificCondition(plutoCond).setSurveyed(false);
        plutoMarket.addCondition(Conditions.HABITABLE);
        plutoMarket.addCondition(Conditions.FARMLAND_ADEQUATE);
        plutoMarket.addCondition(Conditions.RUINS_VAST);
        plutoMarket.addCondition(Conditions.RARE_ORE_ULTRARICH);
        plutoMarket.addCondition(Conditions.ORE_ULTRARICH);
        plutoMarket.setSurveyLevel(MarketAPI.SurveyLevel.NONE);

        GPBaseMegastructure plutoMega = AoTDMegastructureRules.putMegastructure(pluto, "aotd_pluto_station");
        SectorEntityToken station = sys.addCustomEntity(
                PLUTO_STATION_ID,
                "Pluto Mining Station",
                "aotd_pluto_station",
                Factions.NEUTRAL
        );
        station.setCircularOrbitPointingDown(pluto, pluto.getCircularOrbitAngle(), pluto.getRadius() + 220f, pluto.getCircularOrbitPeriod());
        station.getMemoryWithoutUpdate().set(MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY, "aotd_mega");
        MiscellaneousThemeGenerator.makeDiscoverable(station, 40000, 3000f);

        if (plutoMega != null && plutoMega.getSpec() != null) {
            sector.getPlayerMemoryWithoutUpdate().set(
                    "$aotd_mega_system_id_" + plutoMega.getSpec().getMegastructureID(),
                    pluto.getStarSystem().getId()
            );
        }

        PlanetAPI minerva = sys.addPlanet(
                MINERVA_ID,
                sys.getStar(),
                "New Minerva",
                Planets.TUNDRA,
                randAngle(),
                randOrbitRadius(50f, 300f),
                randOrbitRadius(6200f, 9000f),
                randOrbitDays(4500f, 6500f)
        );
        if (newsolMode) {
            String minervaName = RandomNameGenerator.getRandomPlanetName(starName);
            minerva.setName(minervaName);
        }
        MarketAPI minervaMarket = minerva.getMarket();
        minervaMarket.setFactionId(Factions.NEUTRAL);
        minervaMarket.addCondition(Conditions.HABITABLE);
        minervaMarket.addCondition(Conditions.FARMLAND_ADEQUATE);
        minervaMarket.addCondition(Conditions.RUINS_VAST);
        minervaMarket.addCondition(Conditions.VERY_COLD);
        minervaMarket.addCondition(Conditions.RARE_ORE_RICH);
        minervaMarket.addCondition(Conditions.ORE_RICH);
        minervaMarket.addCondition(Conditions.VOLATILES_ABUNDANT);
        minervaMarket.setSurveyLevel(MarketAPI.SurveyLevel.NONE);

        SectorEntityToken gate = sys.addCustomEntity(GATE_ID, "Inactive Gate", "inactive_gate", null);
        gate.setCircularOrbit(sys.getStar(), randAngle(), randOrbitRadius(7000f, 10000f), 90);

        SectorEntityToken buoy = sys.addCustomEntity(BUOY_ID, "Nav Bouy", "nav_buoy", "player");
        buoy.setCircularOrbitPointingDown(sys.getStar(), randAngle(), randOrbitRadius(7000f, 10000f), 90);

        SectorEntityToken relay = sys.addCustomEntity(RELAY_ID, "Comm Relay", "comm_relay", "player");
        relay.setCircularOrbitPointingDown(sys.getStar(), randAngle(), randOrbitRadius(7000f, 10000f), 90);

        SectorEntityToken array = sys.addCustomEntity(ARRAY_ID, "Sensor Array", "sensor_array", "player");
        array.setCircularOrbitPointingDown(sys.getStar(), randAngle(), randOrbitRadius(7000f, 10000f), 90);
    }

    private void cleanupExtraAotdContent() {
        SectorAPI sector = Global.getSector();
        StarSystemAPI kept = sector.getStarSystem(SYSTEM_ID);

        for (StarSystemAPI sys : new ArrayList<>(sector.getStarSystems())) {
            if (sys == null) continue;

            if (sys == kept) {
                for (SectorEntityToken entity : new ArrayList<>(sys.getAllEntities())) {
                    if (entity == null) continue;
                    if (!isAotdEntity(entity)) continue;
                    if (isWhitelistedEntity(entity)) continue;

                    removeEntityAndMarket(sector, sys, entity);
                }
                continue;
            }

            boolean removedAny = false;

            for (SectorEntityToken entity : new ArrayList<>(sys.getAllEntities())) {
                if (entity == null) continue;
                if (!isAotdEntity(entity)) continue;

                removeEntityAndMarket(sector, sys, entity);
                removedAny = true;
            }

            if (removedAny && sys.getAllEntities().size() <= 1) {
                sector.removeStarSystemNextFrame(sys);
            }
        }
    }

    private void removeEntityAndMarket(SectorAPI sector, StarSystemAPI sys, SectorEntityToken entity) {
        MarketAPI market = entity.getMarket();
        if (market != null) {
            entity.setMarket(null);
            if (market.getPrimaryEntity() == entity) {
                market.setPrimaryEntity(null);
            }
            sector.getEconomy().removeMarket(market);
        }
        sys.removeEntity(entity);
    }

    private boolean isAotdEntity(SectorEntityToken entity) {
        String id = entity.getId();
        if (id != null && id.startsWith("aotd_")) return true;

        MarketAPI market = entity.getMarket();
        if (market != null) {
            String marketId = market.getId();
            if (marketId != null && marketId.startsWith("aotd_")) return true;

            for (MarketConditionAPI cond : market.getConditions()) {
                if (cond != null && cond.getId() != null && cond.getId().startsWith("aotd_")) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isWhitelistedEntity(SectorEntityToken entity) {
        String id = entity.getId();
        return SYSTEM_ID.equals(id)
                || STAR_ID.equals(id)
                || PLAYER_PLANET_ID.equals(id)
                || NID_ID.equals(id)
                || PLUTO_ID.equals(id)
                || MINERVA_ID.equals(id)
                || PLUTO_STATION_ID.equals(id)
                || GATE_ID.equals(id)
                || BUOY_ID.equals(id)
                || RELAY_ID.equals(id)
                || ARRAY_ID.equals(id);
    }

    private MarketAPI createMarket(PlanetAPI planet, String marketId, int size, String factionId) {
        MarketAPI market = Global.getFactory().createMarket(marketId, planet.getName(), size);
        market.setPrimaryEntity(planet);
        market.setFactionId(factionId);
        planet.setFaction(factionId);
        planet.setMarket(market);
        Global.getSector().getEconomy().addMarket(market, true);
        return market;
    }

    private void finalizePlayerMarket(MarketAPI market) {
        market.setPlayerOwned(true);
        market.setAdmin(Global.getSector().getPlayerPerson());
        market.setSurveyLevel(MarketAPI.SurveyLevel.FULL);

        for (MarketConditionAPI cond : market.getConditions()) {
            cond.setSurveyed(true);
        }
    }

    private float randAngle() {
        return Misc.random.nextFloat() * 360f;
    }

    private float randOrbitRadius(float min, float max) {
        return min + Misc.random.nextFloat() * (max - min);
    }

    private float randOrbitDays(float min, float max) {
        return min + Misc.random.nextFloat() * (max - min);
    }

    private void patchFactionStartPools(String targetFactionId) {
        NexFactionConfig target = NexConfig.getFactionConfig(targetFactionId);
        if (target == null) return;

        for (NexFactionConfig.StartFleetType type : NexFactionConfig.StartFleetType.values()) {
            NexFactionConfig.StartFleetSet targetSet = target.startShips.get(type);
            if (targetSet == null) {
                targetSet = new NexFactionConfig.StartFleetSet(type);
                target.startShips.put(type, targetSet);
            }

            java.util.Set<String> seen = new java.util.LinkedHashSet<>();

            for (List<String> fleet : targetSet.fleets) {
                if (fleet != null && !fleet.isEmpty()) {
                    seen.add(fleetSignature(fleet));
                }
            }

            for (FactionAPI faction : Global.getSector().getAllFactions()) {
                String factionId = faction.getId();
                NexFactionConfig conf = NexConfig.getFactionConfig(factionId);
                if (conf == null) continue;

                NexFactionConfig.StartFleetSet set = conf.startShips.get(type);
                if (set == null) continue;

                for (List<String> fleet : set.fleets) {
                    if (fleet == null || fleet.isEmpty()) continue;

                    String sig = fleetSignature(fleet);
                    if (seen.add(sig)) {
                        targetSet.addFleet(new ArrayList<>(fleet));
                    }
                }
            }
        }
    }

    private String fleetSignature(List<String> fleet) {
        return String.join("||", fleet);
    }
}