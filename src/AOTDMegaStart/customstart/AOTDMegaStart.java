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

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class AOTDMegaStart extends CustomStart {

    @Override
    public void execute(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        CharacterCreationData data = (CharacterCreationData) memoryMap.get(MemKeys.LOCAL).get("$characterData");
        // enforce normal difficulty
        data.setDifficulty("normal");
        ExerelinSetupData.getInstance().easyMode = false;
        ExerelinSetupData.getInstance().freeStart = true;
        PlayerFactionStore.setPlayerFactionIdNGC(Factions.PLAYER);
        patchFactionStartPools(Factions.PLAYER);
        new Nex_NGCStartFleetOptionsV2().addOptions(dialog, memoryMap);

        data.addScript(new Script() {
            @Override
            public void run() {
                Global.getSector().addScript(new com.fs.starfarer.api.util.DelayedActionScript(0.1f) {
                    @Override
                    public void doAction() {
                        SectorAPI sector = Global.getSector();
                        CampaignFleetAPI fleet = sector.getPlayerFleet();
                        SectorEntityToken home = sector.getEntityById("aotd_perfect_sys_planet");
                        MarketAPI homeMarket = home != null ? home.getMarket() : null;

                        if (fleet != null && home != null) {
                            exerelin.campaign.StartSetupPostTimePass.sendPlayerFleetToLocation(fleet, home);
                        }

                        for (MarketAPI m : new ArrayList<>(sector.getEconomy().getMarketsCopy())) {
                            if (m == null || m == homeMarket) continue;

                            if (Factions.PLAYER.equals(m.getFactionId()) || m.isPlayerOwned()) {
                                if (m.getPrimaryEntity() != null) {
                                    m.getPrimaryEntity().setMarket(null);
                                }
                                sector.getEconomy().removeMarket(m);
                            }
                        }
                    }
                });
            }
        });

        data.addScriptBeforeTimePass(new Script() {
            public void run() {
                Global.getSector().getMemoryWithoutUpdate().set("$aotdPerfectStart", true);
                Global.getSector().getMemoryWithoutUpdate().set("$nex_startLocation", "aotd_perfect_sys_planet");

                SectorAPI sector = Global.getSector();

                String sysid = "new sol";
                StarSystemAPI sys = Global.getSector().createStarSystem(sysid);
                sys.setProcgen(true);
                sys.setName("New Sol");

                boolean randomMode = false;
                if (LunaSettings.getBoolean("megastructure_start", "randomMode")!=null) {
                    randomMode = LunaSettings.getBoolean("megastructure_start", "randomMode");
                }
                float customXpos = -26000f;
                if (LunaSettings.getFloat("megastructure_start", "customXpos")!=null) {
                    customXpos = LunaSettings.getFloat("megastructure_start", "customXpos");
                }
                float customYpos = 11000f;
                if (LunaSettings.getFloat("megastructure_start", "customYpos")!=null) {
                    customYpos = LunaSettings.getFloat("megastructure_start", "customYpos");
                }
                if (randomMode){
                    LocationAPI hyperspace = Global.getSector().getHyperspace();
                    float sysX = hyperspace.getLocation().getX();
                    float sysY = hyperspace.getLocation().getY();
                    Random rand = new Random();
                    sys.initStar("aotd_perfect_sys_star", StarTypes.BLUE_GIANT, 600f, rand.nextFloat() * sysX, rand.nextFloat() * sysY, 600f);
                }
                else {
                    sys.initStar("aotd_perfect_sys_star", StarTypes.BLUE_GIANT, 600f, customXpos, customYpos, 600f);
                }
                //sys.initStar("aotd_perfect_sys_star", StarTypes.BLUE_GIANT, 600f, -26000f, 11000f, 600f);
                sys.generateAnchorIfNeeded();
                sys.autogenerateHyperspaceJumpPoints(true, true);

                sys.addPlanet(
                        "aotd_perfect_sys_planet",
                        sys.getStar(),
                        "New Terra",
                        Planets.PLANET_TERRAN,
                        Misc.random.nextFloat() * 360f,
                        200f + Misc.random.nextFloat() * 100f, // orbit radius
                        2000f + Misc.random.nextFloat() * 500f, // distance from star
                        350f // size
                );


                SectorEntityToken planet = sys.getEntityById("aotd_perfect_sys_planet");

                MarketAPI market = Global.getFactory().createMarket(
                        "aotd_perfect_sys_colony", planet.getName(), 5);
                market.setPrimaryEntity(planet);
                market.setFactionId(Factions.PLAYER);
                planet.setFaction(Factions.PLAYER);

                market.addCondition(Conditions.HABITABLE);
                market.addCondition(Conditions.MILD_CLIMATE);
                market.addCondition(Conditions.FARMLAND_RICH);
                market.addCondition(Conditions.ORE_RICH);
                market.addCondition(Conditions.RARE_ORE_RICH);
                market.addCondition(Conditions.ORGANICS_PLENTIFUL);
                market.addCondition(Conditions.POPULATION_5);
                market.addIndustry(Industries.POPULATION);
                market.addIndustry(Industries.SPACEPORT);
                market.addIndustry(Industries.WAYSTATION);
                market.addIndustry(Industries.BATTLESTATION);
                market.addIndustry(Industries.FARMING);
                market.addIndustry(Industries.MINING);
                market.addIndustry(Industries.COMMERCE);
                market.addIndustry(Industries.PATROLHQ);
                market.addIndustry(Industries.GROUNDDEFENSES);
                market.addSubmarket(Submarkets.LOCAL_RESOURCES);
                market.addSubmarket(Submarkets.SUBMARKET_OPEN);
                market.addSubmarket(Submarkets.SUBMARKET_STORAGE);

                Global.getSector().getEconomy().addMarket(market, true);
                planet.setMarket(market);

                market.setPlayerOwned(true);
                market.setAdmin(sector.getPlayerPerson());

                market.setSurveyLevel(MarketAPI.SurveyLevel.FULL);
                for (MarketConditionAPI cond : market.getConditions())
                {
                    cond.setSurveyed(true);
                }
                market.setAdmin(sector.getPlayerPerson());

                // 1) Coronal Hypershunt
                CustomEntitySpecAPI spec = Global.getSettings().getCustomEntitySpec(Entities.CORONAL_TAP);
                BaseThemeGenerator.EntityLocation loc = new BaseThemeGenerator.EntityLocation();
                float orbitRadius = sys.getStar().getRadius() + spec.getDefaultRadius() + 100f;
                float orbitDays = orbitRadius / 20f;
                Random random = new Random();
                loc.orbit = Global.getFactory().createCircularOrbitPointingDown(sys.getStar(), random.nextFloat() * 360f, orbitRadius, orbitDays);
                BaseThemeGenerator.AddedEntity entity = BaseThemeGenerator.addEntity(random, sys, loc, Entities.CORONAL_TAP, Factions.NEUTRAL);
                
                // Create a new nidavelir
                PlanetAPI nidavelir = sys.addPlanet(
                        "aotd_nidavelir",
                        sys.getStar(),
                        "New Mars",
                        Planets.ARID,
                        Misc.random.nextFloat() * 360f,
                        100f + Misc.random.nextFloat() * 100f, // orbit radius
                        3000f + Misc.random.nextFloat() * 500f, // distance from star
                        350f // size
                );
                GPBaseMegastructure mega = AoTDMegastructureRules.putMegastructure(nidavelir, "aotd_nidavelir");
                nidavelir.addTag(Tags.NOT_RANDOM_MISSION_TARGET);
                String token = nidavelir.getMarket().addCondition("aotd_nidavelir_complex");
                nidavelir.getMarket().getSpecificCondition(token).setSurveyed(false);
                nidavelir.getMarket().addCondition(Conditions.HABITABLE);
                nidavelir.getMarket().addCondition(Conditions.FARMLAND_ADEQUATE);
                nidavelir.getMarket().addCondition(Conditions.RUINS_VAST);
                nidavelir.getMarket().addCondition(Conditions.VERY_HOT);
                nidavelir.getMarket().addCondition(Conditions.RARE_ORE_ULTRARICH);
                nidavelir.getMarket().addCondition(Conditions.ORE_ULTRARICH);
                Global.getSector().getPlayerMemoryWithoutUpdate().set("$aotd_mega_system_id_"+mega.getSpec().getMegastructureID(),nidavelir.getStarSystem().getId());

                // Create a new nidavelir
                PlanetAPI pluto = sys.addPlanet(
                        "aotd_pluto",
                        sys.getStar(),
                        "New Pluto",
                        Planets.ARID,
                        Misc.random.nextFloat() * 360f,
                        100f + Misc.random.nextFloat() * 100f, // orbit radius
                        4000f + Misc.random.nextFloat() * 500f, // distance from star
                        350f // size
                );
                pluto.getMarket().addCondition(Conditions.HABITABLE);
                pluto.getMarket().addCondition(Conditions.FARMLAND_ADEQUATE);
                pluto.getMarket().addCondition(Conditions.RUINS_VAST);
                pluto.getMarket().addCondition(Conditions.RARE_ORE_ULTRARICH);
                pluto.getMarket().addCondition(Conditions.ORE_ULTRARICH);

                String t = pluto.getMarket().addCondition("aotd_pluto_station");
                pluto.getMarket().getSpecificCondition(t).setSurveyed(false);
                GPBaseMegastructure mega1 = AoTDMegastructureRules.putMegastructure(pluto, "aotd_pluto_station");
                SectorEntityToken token1 = pluto.getMarket().getStarSystem().addCustomEntity("aotd_pluto_station", "Pluto Mining Station", "aotd_pluto_station", Factions.NEUTRAL);
                float angle = pluto.getCircularOrbitAngle();
                float period = pluto.getCircularOrbitPeriod(); // 270 : height
                token1.setCircularOrbitPointingDown(pluto, angle, pluto.getRadius() + 270 + 70, period);
                token1.getMemoryWithoutUpdate().set(MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY, "aotd_mega");
                MiscellaneousThemeGenerator.makeDiscoverable(token1, 40000, 3000f);

                // Create a very cold tundra
                PlanetAPI minerva = sys.addPlanet(
                        "aotd_minerva",
                        sys.getStar(),
                        "New Minerva",
                        Planets.TUNDRA,
                        Misc.random.nextFloat() * 360f,
                        100f + Misc.random.nextFloat() * 100f, // orbit radius
                        5000f + Misc.random.nextFloat() * 500f, // distance from star
                        350f // size
                );
                minerva.getMarket().addCondition(Conditions.HABITABLE);
                minerva.getMarket().addCondition(Conditions.FARMLAND_ADEQUATE);
                minerva.getMarket().addCondition(Conditions.RUINS_VAST);
                minerva.getMarket().addCondition(Conditions.VERY_COLD);
                minerva.getMarket().addCondition(Conditions.RARE_ORE_RICH);
                minerva.getMarket().addCondition(Conditions.ORE_RICH);
                minerva.getMarket().addCondition(Conditions.VOLATILES_ABUNDANT);

                //Gate
                SectorEntityToken calvera_gate = sys.addCustomEntity("aotd_perfect_sys_gate",
                        "Gate",
                        "inactive_gate",
                        null);
                calvera_gate.setCircularOrbit(sys.getStar(), Misc.random.nextFloat() * 360f, 4000f + Misc.random.nextFloat() * 500f, 90); //focus, angle, orbit radius, orbit days

                //Buoy
                SectorEntityToken buoy = sys.addCustomEntity("aotd_perfect_sys_buoy",
                        "Buoy",
                        "nav_buoy",
                        "player");
                buoy.setCircularOrbitPointingDown(sys.getStar(), Misc.random.nextFloat() * 360f, 4000f + Misc.random.nextFloat() * 500f, 90); //focus, angle, orbit radius, orbit days

                //Relay
                SectorEntityToken relay = sys.addCustomEntity("aotd_perfect_sys_relay",
                        "Relay",
                        "comm_relay",
                        "player");
                relay.setCircularOrbitPointingDown(sys.getStar(), Misc.random.nextFloat() * 360f, 4000f + Misc.random.nextFloat() * 500f, 90); //focus, angle, orbit radius, orbit days

                //Array
                SectorEntityToken array = sys.addCustomEntity("aotd_perfect_sys_array",
                        "Array",
                        "sensor_array",
                        "player");
                array.setCircularOrbitPointingDown(sys.getStar(), Misc.random.nextFloat() * 360f, 4000f + Misc.random.nextFloat() * 500f, 90); //focus, angle, orbit radius, orbit days
            }
        });
    }

    private void patchFactionStartPools(String targetFactionId) {
        NexFactionConfig target = NexConfig.getFactionConfig(targetFactionId);
        if (target == null) return;

        target.startShips.clear();

        for (NexFactionConfig.StartFleetType type : NexFactionConfig.StartFleetType.values()) {
            NexFactionConfig.StartFleetSet mergedSet = new NexFactionConfig.StartFleetSet(type);

            for (FactionAPI faction : Global.getSector().getAllFactions()) {
                String factionId = faction.getId();
                NexFactionConfig conf = NexConfig.getFactionConfig(factionId);
                if (conf == null) continue;

                NexFactionConfig.StartFleetSet set = conf.startShips.get(type);
                if (set == null) continue;

                for (List<String> fleet : set.fleets) {
                    if (fleet == null || fleet.isEmpty()) continue;
                    mergedSet.addFleet(new ArrayList<>(fleet));
                }
            }

            if (!mergedSet.fleets.isEmpty()) {
                target.startShips.put(type, mergedSet);
            }
        }
    }
}