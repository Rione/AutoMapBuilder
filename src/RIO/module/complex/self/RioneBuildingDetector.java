package RIO.module.complex.self;

import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.MessageUtil;
import adf.agent.communication.standard.bundle.centralized.CommandAmbulance;
import adf.agent.communication.standard.bundle.centralized.CommandFire;
import adf.agent.communication.standard.bundle.information.MessageAmbulanceTeam;
import adf.agent.communication.standard.bundle.information.MessageBuilding;
import adf.agent.communication.standard.bundle.information.MessageFireBrigade;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.communication.CommunicationMessage;
import adf.component.module.algorithm.Clustering;
import adf.component.module.complex.BuildingDetector;
import rescuecore2.config.Config;
import rescuecore2.standard.entities.*;
import rescuecore2.standard.entities.StandardEntityConstants.Fieryness;
import rescuecore2.standard.kernel.comms.ChannelCommunicationModel;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;

import static rescuecore2.standard.entities.StandardEntityURN.*;

public class RioneBuildingDetector extends BuildingDetector
{
    private EntityID result;
    
    private Clustering clustering;
    
    //private Simulator simulator;
    
    private Collection<EntityID> agentPositions;
    private Map<EntityID, Integer> sentTimeMap;
    private int sendingAvoidTimeReceived;
    private int sendingAvoidTimeSent;
    
    
    private int maxExtinguishPower;
    
    // flag & communication
    private boolean isRadio = true;
    private int channelMax = 0;
    int voice = 256;
    int voiceCount = 1;
    private int bandwidth = 1024;
    private int devidedBandwidth;
    // Tactics~ で使われている帯域量 は 30Byte とする
    private final int USED_BANDWIDTH = 30;
    
    public RioneBuildingDetector(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData)
    {
        super(ai, wi, si, moduleManager, developData);
        switch (si.getMode())
        {
            case PRECOMPUTATION_PHASE:
                this.clustering = moduleManager.getModule("RIOBuildingDetector.Clustering", "RIO.module.algorithm.RioneKmeansPP");
                break;
            case PRECOMPUTED:
                this.clustering = moduleManager.getModule("RIOBuildingDetector.Clustering", "RIO.module.algorithm.RioneKmeansPP");
                break;
            case NON_PRECOMPUTE:
                this.clustering = moduleManager.getModule("RIOBuildingDetector.Clustering", "RIO.module.algorithm.RioneKmeansPP");
                break;
        }
        registerModule(this.clustering);
        
        this.agentPositions = new HashSet<>();
        this.sentTimeMap = new HashMap<>();
        //this.simulator = new Simulator();
        this.maxExtinguishPower = scenarioInfo.getFireExtinguishMaxSum();
        //radio
        this.channelMax = this.scenarioInfo.getCommsChannelsCount();
        if(channelMax < 2) isRadio = false; // 最大チャンネル数が2以下で通信不可
        Config config = this.scenarioInfo.getRawConfig();
        bandwidth = config.getIntValue(ChannelCommunicationModel.PREFIX+1+".bandwidth");
        voice = config.getIntValue(ChannelCommunicationModel.PREFIX+0+".messages.size");
        voiceCount = config.getIntValue(ChannelCommunicationModel.PREFIX+0+".messages.max");
        int numAgents = this.worldInfo.getEntitiesOfType(AMBULANCE_TEAM,FIRE_BRIGADE,POLICE_FORCE).size();
        int numCenter = this.worldInfo.getEntitiesOfType(StandardEntityURN.AMBULANCE_CENTRE, StandardEntityURN.FIRE_STATION, StandardEntityURN.POLICE_OFFICE).size();
        this.devidedBandwidth = bandwidth / (numAgents + numCenter);
    }
    
    
    @Override
    public BuildingDetector updateInfo(MessageManager messageManager)
    {
    	/*if(this.result != null) {
            Building building = (Building)this.worldInfo.getEntity(this.result);
            if(building.getFieryness() >= 1) {
            	System.out.println(building.getFieryness()+" ID:"+ agentInfo.me().getID().getValue());
            	
                    CommandFire message = new CommandFire(
                            true,
                            agentInfo.me().getID(),
                            building.getID(),
                            CommandFire.ACTION_AUTONOMY
                    );
                    messageManager.addMessage(message);
            
        }
            }*/
        
        //simulator.step();
        
        // 帯域の制限
        if(channelMax >= 2) isRadio = true;
        
        // 視界情報の更新
        Set<EntityID> changedEntities = this.worldInfo.getChanged().getChangedEntities();
        changedEntities.add(this.agentInfo.me().getID());
        
        StandardEntity myEntity = this.agentInfo.me();
        StandardEntity nearAT = null;
        StandardEntity nearPF = null;
        List<Building> burningBuildings = new ArrayList<Building>();
        
        // 都度for文を回さないように,changedEntitiesの参照を極力まとめる
        for(EntityID id : changedEntities){
            StandardEntity entity = this.worldInfo.getEntity(id);
            
            // 視界内のATを一人取得(一番最後のAT)
            if(entity instanceof AmbulanceTeam){
                nearAT = entity;
            }
            // 視界内のPFを一人取得(一番最後のPF)
            else if(entity instanceof PoliceForce){
                nearPF = entity;
            }
            // 視界内の燃えている建物を取得
            else if(entity instanceof Building && ((Building) entity).isOnFire()){
                burningBuildings.add((Building)entity);
            }
        }
        
        // ATに救助命令を送信（できているか不明）
        if(nearAT != null && ((Human)myEntity).getBuriedness() > 0){
            messageManager.addMessage(new CommandAmbulance(isRadio, nearAT.getID(), myEntity.getID(), MessageAmbulanceTeam.ACTION_RESCUE));
        }
        
        // FBに消火命令を送信（できているか不明）
        if(burningBuildings.size() > 0){
            messageManager.addMessage(new CommandFire(isRadio, null, burningBuildings.get(0).getID(), CommandFire.ACTION_EXTINGUISH));
        }
        
        FireBrigade agent = (FireBrigade) agentInfo.me();
        for(CommunicationMessage message: messageManager.getReceivedMessageList()){
            if(message instanceof MessageFireBrigade){
                MessageFireBrigade messageFB = (MessageFireBrigade) message;
                if(messageFB.getAction() == 2){ //ActionExtinguish
                    
                    /*
                    //for simulator
                    RIOBuilding building = simulator.getRioBuilding(worldInfo.getEntity(messageFB.getTargetID()));
                    int w = (int) ((double) building.getWaterNeeded() * 1.0);
                    w = Math.min(w, this.maxExtinguishPower);
                    w = Math.min(w, agent.getWater() - 1);
                    building.setWaterQuantity(w);
                    */
                }
            }else  if(message instanceof MessageBuilding){
                MessageBuilding mb = (MessageBuilding)message;
                if(!changedEntities.contains(mb.getBuildingID())) {
                    MessageUtil.reflectMessage(this.worldInfo, mb);
                }
            }
        }
        
        super.updateInfo(messageManager);
        if (this.getCountUpdateInfo() >= 2)
        {
            return this;
        }
        
        return this;
    }
    
    @Override
    public BuildingDetector calc()
    {
        
        FireBrigade agent = (FireBrigade)this.agentInfo.me();
        
        /*
        //For simulator
        
        if(simulator == null){
            System.out.print("#ID:" + agent.getID().getValue() + " Simulator is NULL!!");
        }else{
            for (Building building: worldInfo.getFireBuildings()){
                System.out.println("#ID:" + agent.getID().getValue() +
                        " Building(" + building.getID().getValue() + ")");
                
                if(simulator.getRioBuilding(building)!=null){
                    System.out.println(" EstTemp: " + simulator.getRioBuilding(building)._getTemperture() +
                            " TheTemp: " + building.getTemperature());
                }else{
                    System.out.println("RIOBuilding is NULL!!");
                }
                
            }
        }
        */
        
        
        this.result = this.calcInSight();
        if(this.result!=null) {
            return this;
        }
        this.result = this.calcTargetInCluster();
        if (this.result == null)
        {
            this.result = this.calcTargetInWorld();
        }
        return this;
    }
    //nishida
    //視界内の燃えてる建物を優先
    private EntityID calcInSight() {
        Collection<EntityID> en = this.worldInfo.getChanged().getChangedEntities();
        List<StandardEntity> ses = new ArrayList<>();
        for(EntityID entity:en) {
            StandardEntity se = this.worldInfo.getEntity(entity);
            ses.add(se);
        }
        if(!ses.isEmpty()) {
            List<Building> targets = new ArrayList<Building>();
            targets = filterFiery(ses);
            if(targets!=null && !targets.isEmpty()) {
                Collections.sort(targets, new DistanceSorter(worldInfo, agentInfo.me()));
                Building selectedBuilding = targets.get(0);
                return selectedBuilding.getID();
            }
        }
        return null;
    }
    
    private List<Building> filterFiery(Collection<? extends StandardEntity> input) {
        ArrayList<Building> fireBuildings = new ArrayList<>();
        for (StandardEntity entity : input) {
            if (entity instanceof Building && ((Building) entity).isOnFire()) {
                fireBuildings.add((Building) entity);
            }
        }
        if(!fireBuildings.isEmpty())
            return filterFieryness(fireBuildings);
        return null;
    }
    //nishida
    //燃焼度の低いものを優先
    private  List<Building> filterFieryness(Collection<? extends StandardEntity> input){
        ArrayList<Building> fireBuildings = new ArrayList<>();
        if(!input.isEmpty()) {
            for (StandardEntity entity : input) {
                if (entity instanceof Building
                        && ((Building) entity).isOnFire()
                        &&((Building) entity).getFierynessEnum() == Fieryness.HEATING) {
                    fireBuildings.add((Building) entity);
                }
            }
            if(!fireBuildings.isEmpty())
                return fireBuildings;
            
            for (StandardEntity entity : input) {
                if (entity instanceof Building
                        && ((Building) entity).isOnFire()
                        &&((Building) entity).getFierynessEnum() == Fieryness.BURNING) {
                    fireBuildings.add((Building) entity);
                }
            }
            if(!fireBuildings.isEmpty())
                return fireBuildings;
            
            for (StandardEntity entity : input) {
                if (entity instanceof Building
                        && ((Building) entity).isOnFire()
                        &&((Building) entity).getFierynessEnum() == Fieryness.INFERNO) {
                    fireBuildings.add((Building) entity);
                }
            }
            return fireBuildings;
        }
        return null;
    }
    
    private EntityID calcTargetInCluster()
    {
        int clusterIndex = this.clustering.getClusterIndex(this.agentInfo.getID());
        Collection<StandardEntity> elements = this.clustering.getClusterEntities(clusterIndex);
        if (elements == null || elements.isEmpty())
        {
            return null;
        }
        StandardEntity me = this.agentInfo.me();
        List<StandardEntity> agents = new ArrayList<>(this.worldInfo.getEntitiesOfType(StandardEntityURN.FIRE_BRIGADE));
        Set<StandardEntity> fireBuildings = new HashSet<>();
        for (StandardEntity entity : elements)
        {
            if (entity instanceof Building && ((Building) entity).isOnFire())
            {
                fireBuildings.add(entity);
            }
        }
        
        for (StandardEntity entity : fireBuildings)
        {
            if (agents.isEmpty())
            {
                break;
            }
            else if (agents.size() == 1)
            {
                if (agents.get(0).getID().getValue() == me.getID().getValue())
                {
                    return entity.getID();
                }
                break;
            }
            agents.sort(new DistanceSorter(this.worldInfo, entity));
            StandardEntity a0 = agents.get(0);
            StandardEntity a1 = agents.get(1);
            
            if (me.getID().getValue() == a0.getID().getValue() || me.getID().getValue() == a1.getID().getValue())
            {
                return entity.getID();
            }
            else
            {
                agents.remove(a0);
                agents.remove(a1);
            }
        }
        return null;
    }
    
    //nishida
    private EntityID calcTargetInWorld(){
        Collection<StandardEntity> ses = this.worldInfo.getEntitiesOfType(
                StandardEntityURN.BUILDING,
                StandardEntityURN.GAS_STATION,
                StandardEntityURN.AMBULANCE_CENTRE,
                StandardEntityURN.FIRE_STATION,
                StandardEntityURN.POLICE_OFFICE
        );
        List<Building> targets = new ArrayList<Building>();
        targets = filterFiery(ses);
        if(targets!=null && !targets.isEmpty()) {
            Collections.sort(targets, new DistanceSorter(worldInfo, agentInfo.me()));
            Building selectedBuilding = targets.get(0);
            return selectedBuilding.getID();
        }
        return null;
    }
    
    
    @Override
    public EntityID getTarget()
    {
        return this.result;
    }
    
    @Override
    public BuildingDetector precompute(PrecomputeData precomputeData)
    {
        super.precompute(precomputeData);
        System.out.println("precomputed");
        //this.simulator.init();
        if (this.getCountPrecompute() >= 2)
        {
            return this;
        }
        return this;
    }
    
    @Override
    public BuildingDetector resume(PrecomputeData precomputeData)
    {
        super.resume(precomputeData);
        System.out.println("resumed");
        //this.simulator.init();
        if (this.getCountPrecompute() >= 2)
        {
            return this;
        }
        return this;
    }
    
    @Override
    public BuildingDetector preparate()
    {
        super.preparate();
        System.out.println("preparated");
        //this.simulator.init();
        if (this.getCountPrecompute() >= 2)
        {
            return this;
        }
        return this;
    }
    
    private class DistanceSorter implements Comparator<StandardEntity>
    {
        private StandardEntity reference;
        private WorldInfo worldInfo;
        
        DistanceSorter(WorldInfo wi, StandardEntity reference)
        {
            this.reference = reference;
            this.worldInfo = wi;
        }
        
        public int compare(StandardEntity a, StandardEntity b)
        {
            int d1 = this.worldInfo.getDistance(this.reference, a);
            int d2 = this.worldInfo.getDistance(this.reference, b);
            return d1 - d2;
        }
    }
    private boolean isInBlockade(Human human) {
        if(!human.isXDefined() || !human.isXDefined()) return false;
        int agentX = human.getX();
        int agentY = human.getY();
        StandardEntity positionEntity = this.worldInfo.getPosition(human);
        if(positionEntity instanceof Road){
            Road road = (Road)positionEntity;
            if(road.isBlockadesDefined() && road.getBlockades().size() > 0){
                for(Blockade blockade : worldInfo.getBlockades(road)){
                    if(blockade.getShape().contains(agentX, agentY)){
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
