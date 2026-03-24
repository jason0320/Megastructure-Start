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
import com.fs.starfarer.api.impl.campaign.procgen.themes.MiscellaneousThemeGenerator;
import com.fs.starfarer.api.impl.campaign.rulecmd.AoTDMegastructureRules;
import com.fs.starfarer.api.impl.campaign.rulecmd.newgame.Nex_NGCStartFleetOptionsV2;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.vok.campaign.econ.globalproduction.models.megastructures.GPBaseMegastructure;
import exerelin.campaign.ExerelinSetupData;
import exerelin.campaign.PlayerFactionStore;
import exerelin.campaign.customstart.CustomStart;
import exerelin.utilities.StringHelper;

import java.util.ArrayList;
import java.util.Map;

public class AOTDMegaStart extends CustomStart {

    @Override
    public void execute(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        CharacterCreationData data = (CharacterCreationData) memoryMap.get(MemKeys.LOCAL).get("$characterData");
        // enforce normal difficulty
        data.setDifficulty("normal");
        ExerelinSetupData.getInstance().easyMode = false;
        PlayerFactionStore.setPlayerFactionIdNGC(Factions.PLAYER);

        // open Nexerelin's custom fleet picker instead of adding a fixed ship
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

                String id = "aotd_perfect_sys";
                StarSystemAPI sys = Global.getSector().createStarSystem(id);
                sys.setProcgen(true);
                sys.setName("New Sol");
                sys.initStar("aotd_perfect_sys_star", StarTypes.BLUE_GIANT, 600f, -26000f, 11000f, 600f);
                sys.generateAnchorIfNeeded();
                sys.autogenerateHyperspaceJumpPoints(true, true);

                sys.addPlanet(
                        "aotd_perfect_sys_planet",
                        sys.getStar(),
                        "New Terra",
                        Planets.PLANET_TERRAN,
                        Misc.random.nextFloat() * 360f,
                        200f + Misc.random.nextFloat() * 100f, // orbit radius
                        2500f + Misc.random.nextFloat() * 500f, // distance from star
                        350f // size
                );


                SectorEntityToken planet = sys.getEntityById("aotd_perfect_sys_planet");

                MarketAPI market = Global.getFactory().createMarket(
                        "aotd_perfect_sys_colony", planet.getName(), 6);
                market.setPrimaryEntity(planet);
                market.setFactionId(Factions.PLAYER);
                planet.setFaction(Factions.PLAYER);

                market.addCondition(Conditions.HABITABLE);
                market.addCondition(Conditions.MILD_CLIMATE);
                market.addCondition(Conditions.FARMLAND_RICH);
                market.addCondition(Conditions.ORE_RICH);
                market.addCondition(Conditions.RARE_ORE_RICH);
                market.addCondition(Conditions.ORGANICS_PLENTIFUL);
                market.addCondition(Conditions.POPULATION_6);
                market.addIndustry(Industries.POPULATION);
                market.addIndustry(Industries.SPACEPORT);
                market.addIndustry(Industries.BATTLESTATION);
                market.addIndustry(Industries.FARMING);
                market.addIndustry(Industries.MINING);
                market.addIndustry(Industries.HEAVYINDUSTRY);
                market.addIndustry(Industries.LIGHTINDUSTRY);
                market.addIndustry(Industries.PATROLHQ);
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
                SectorEntityToken coronal = sys.addCustomEntity(
                        "aotd_coronal_hypershunt",
                        "Coronal Hypershunt",
                        Entities.CORONAL_TAP,
                        Factions.NEUTRAL);
                coronal.setCircularOrbit(
                        sys.getStar(),
                        Misc.random.nextFloat() * 360f,
                        sys.getStar().getRadius() + 100f,
                        200f
                );

                // Create a new nidavelir
                PlanetAPI nidavelir = sys.addPlanet(
                        "aotd_nidavelir",
                        sys.getStar(),
                        "New Mars",
                        Planets.BARREN,
                        Misc.random.nextFloat() * 360f,
                        200f + Misc.random.nextFloat() * 100f, // orbit radius
                        3000f + Misc.random.nextFloat() * 500f, // distance from star
                        350f // size
                );
                GPBaseMegastructure mega = AoTDMegastructureRules.putMegastructure(nidavelir, "aotd_nidavelir");
                nidavelir.addTag(Tags.NOT_RANDOM_MISSION_TARGET);
                String token = nidavelir.getMarket().addCondition("aotd_nidavelir_complex");
                nidavelir.getMarket().getSpecificCondition(token).setSurveyed(false);
                nidavelir.getMarket().removeCondition(Conditions.RUINS_EXTENSIVE);
                nidavelir.getMarket().removeCondition(Conditions.RUINS_SCATTERED);
                nidavelir.getMarket().removeCondition(Conditions.RUINS_WIDESPREAD);
                if (!nidavelir.getMarket().hasCondition(Conditions.RUINS_VAST)) {
                    nidavelir.getMarket().addCondition(Conditions.RUINS_VAST);
                }
                nidavelir.getMarket().addCondition(Conditions.VERY_HOT);
                nidavelir.getMarket().removeCondition(Conditions.HOT);
                nidavelir.getMarket().removeCondition(Conditions.COLD);
                nidavelir.getMarket().removeCondition(Conditions.VERY_COLD);
                nidavelir.getMarket().addCondition(Conditions.RARE_ORE_ULTRARICH);
                nidavelir.getMarket().addCondition(Conditions.ORE_ULTRARICH);
                Global.getSector().getPlayerMemoryWithoutUpdate().set("$aotd_mega_system_id_"+mega.getSpec().getMegastructureID(),nidavelir.getStarSystem().getId());

                // Create a new nidavelir
                PlanetAPI pluto = sys.addPlanet(
                        "aotd_pluto",
                        sys.getStar(),
                        "New Pluto",
                        Planets.BARREN,
                        Misc.random.nextFloat() * 360f,
                        200f + Misc.random.nextFloat() * 100f, // orbit radius
                        3500f + Misc.random.nextFloat() * 500f, // distance from star
                        350f // size
                );
                pluto.getMarket().addCondition(Conditions.VERY_HOT);
                pluto.getMarket().removeCondition(Conditions.HOT);
                pluto.getMarket().removeCondition(Conditions.COLD);
                pluto.getMarket().removeCondition(Conditions.VERY_COLD);
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

                // Create a gas giant
                PlanetAPI jupiter = sys.addPlanet(
                        "aotd_gas",
                        sys.getStar(),
                        "New Jupiter",
                        Planets.GAS_GIANT,
                        Misc.random.nextFloat() * 360f,
                        200f + Misc.random.nextFloat() * 100f, // orbit radius
                        4000f + Misc.random.nextFloat() * 500f, // distance from star
                        350f // size
                );
                jupiter.getMarket().addCondition(Conditions.VERY_HOT);
                jupiter.getMarket().addCondition(Conditions.VOLATILES_ABUNDANT);
                jupiter.getMarket().addCondition(Conditions.HIGH_GRAVITY);

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
}