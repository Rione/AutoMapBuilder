package adf.sample.module.complex;

import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.MessageUtil;
import adf.agent.communication.standard.bundle.centralized.CommandPolice;
import adf.agent.communication.standard.bundle.information.MessageAmbulanceTeam;
import adf.agent.communication.standard.bundle.information.MessageBuilding;
import adf.agent.communication.standard.bundle.information.MessageCivilian;
import adf.agent.communication.standard.bundle.information.MessageFireBrigade;
import adf.agent.communication.standard.bundle.information.MessagePoliceForce;
import adf.agent.communication.standard.bundle.information.MessageRoad;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.communication.CommunicationMessage;
import adf.component.module.algorithm.Clustering;
import adf.component.module.algorithm.PathPlanning;
import adf.component.module.complex.RoadDetector;
import rescuecore2.config.Config;
import rescuecore2.standard.entities.*;
import rescuecore2.standard.entities.StandardEntityConstants.Fieryness;
import rescuecore2.standard.kernel.comms.ChannelCommunicationModel;
import rescuecore2.worldmodel.EntityID;

import java.util.*;
import java.util.stream.Collectors;

import static rescuecore2.standard.entities.StandardEntityURN.*;

public class SampleRoadDetector extends RoadDetector
{
    // priorities
	private Map<EntityID, Double> targetPriorities;
	
	private Set<EntityID> entrances;
	private Set<EntityID> intersections;
	
	private Set<EntityID> clearedRoads;
    private ArrayList<Human> buriedHumans;
    
    private Map<EntityID, Integer> previousBlockadesNumber;
    
    // flag & communication
    private boolean haveOffice = false; 
    private boolean isRadio = true;
    private int channelMax = 0;
    int voice = 256;
    int voiceCount = 1;
    private int bandwidth = 1024;
    private int devidedBandwidth;
    // TacticsPolice で使われている帯域量 は 30Byte とする
    private final int USED_BANDWIDTH = 30;
    
    // tools
    private PathPlanning pathPlanning;
    private Clustering clustering;
    
    // result
    private EntityID result;

    public SampleRoadDetector(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData)
    {
        super(ai, wi, si, moduleManager, developData);
        switch (scenarioInfo.getMode())
        {
            case PRECOMPUTATION_PHASE:
                this.pathPlanning = moduleManager.getModule("SampleRoadDetector.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                break;
            case PRECOMPUTED:
                this.pathPlanning = moduleManager.getModule("SampleRoadDetector.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                break;
            case NON_PRECOMPUTE:
                this.pathPlanning = moduleManager.getModule("SampleRoadDetector.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                break;
        }
        registerModule(this.pathPlanning);
        
        this.haveOffice = !wi.getEntityIDsOfType(StandardEntityURN.POLICE_OFFICE).isEmpty();      
        communicationInitialize();
        
        entrances = new HashSet<>();
        intersections = new HashSet<>();
        clearedRoads = new HashSet<>();
        buriedHumans = new ArrayList<>();
        previousBlockadesNumber = new HashMap<>();
        result = null;
    }

    @Override
    public RoadDetector calc()
    {
    	if(this.result == null) {
            EntityID positionID = this.agentInfo.getPosition();
            if(this.targetPriorities.containsKey(positionID) && this.targetPriorities.get(positionID) > 0){
                this.result = positionID;
                return this;
            }
            if(this.agentInfo.getTime() < 5 && this.clustering.getClusterIndex(this.agentInfo.me().getID()) != this.clustering.getClusterIndex(this.agentInfo.getPosition())){
            	resultToCluster();
            }else{ // agentInfo.getTime() >= 5
            	selectTop();
            }
        }else if(this.result.getValue() == this.agentInfo.getPosition().getValue()){
        	Set<EntityID> sameRoadAgents = this.worldInfo.getChanged().getChangedEntities().stream()
            		.filter(id -> this.worldInfo.getEntity(id) instanceof AmbulanceTeam || this.worldInfo.getEntity(id) instanceof FireBrigade || this.worldInfo.getEntity(id) instanceof PoliceForce)
            		.filter(id -> isInBlockade((Human)this.worldInfo.getEntity(id)))
            		.filter(id -> this.worldInfo.getPosition(id).getID().getValue() == this.agentInfo.getPosition().getValue())
            		.collect(Collectors.toSet());
        	if(sameRoadAgents.size() == 0){
        		selectTop();
        	}
        }
        return this;
    }
    
    private void resultToCluster() {
    	int index = this.clustering.getClusterIndex(this.agentInfo.me().getID());
    	this.clustering.getClusterEntities(index).stream() //林追加部分
    		.filter(se -> se instanceof Refuge)
    		.map(se -> ((Area)se).getNeighbours())
		.	forEach(set -> {
    			set.stream()
    				.filter(ne -> worldInfo.getEntity(ne) instanceof Road)
    				.forEach(id -> this.result = id);
			});
    	if(this.result == null){
    		this.clustering.getClusterEntities(index).stream()
    			.filter(se -> se instanceof Road)
    			.map(se -> se.getID())
    			.limit(1)
    			.forEach(id -> this.result = id);
    	}
	}
    
    private void selectTop() {
    	targetPriorities.entrySet().stream()
	        .sorted(java.util.Collections.reverseOrder(java.util.Map.Entry.comparingByValue()))
	        .limit(1)
	        .forEach(e -> this.result = e.getKey());
	}

    @Override
    public EntityID getTarget() {
        return this.result;
    }

    @Override
    public RoadDetector precompute(PrecomputeData precomputeData) {
        super.precompute(precomputeData);
        if(this.getCountPrecompute() >= 2) {
            return this;
        }
        this.pathPlanning.precompute(precomputeData);
        this.clustering.precompute(precomputeData);
        return this;
    }

    @Override
    public RoadDetector resume(PrecomputeData precomputeData) {
        super.resume(precomputeData);
        if(this.getCountResume() >= 2) {
            return this;
        }
        this.pathPlanning.resume(precomputeData);
        this.clustering.resume(precomputeData);
        initializeSets();
        this.targetPriorities = new Hashtable<>();
        return this;
    }

    @Override
    public RoadDetector preparate() {
        super.preparate();
        if(this.getCountPreparate() >= 2) {
            return this;
        }
        this.pathPlanning.preparate();
        this.clustering.preparate();
        initializeSets();
        this.targetPriorities = new Hashtable<>();
        return this;
    }

    @Override
    public RoadDetector updateInfo(MessageManager messageManager)
    {
        super.updateInfo(messageManager);
        if (this.getCountUpdateInfo() >= 2)
        {
            return this;
        }
        this.pathPlanning.updateInfo(messageManager);
    	this.clustering.updateInfo(messageManager);
    	
    	ArrayList<Human> activeHumans = new ArrayList<>();  	
    	
    	// 帯域の制限
    	int limitBandwidth = devidedBandwidth - USED_BANDWIDTH;
    	int limitVoice = voice;
        int limitVoiceCount = voiceCount;
        if(channelMax >= 2) isRadio = true;
	        
    	// 視界情報の更新
        Set<EntityID> changedEntities = this.worldInfo.getChanged().getChangedEntities();
        changedEntities.add(this.agentInfo.me().getID());
        
        // 視界情報からclearedRoadsを更新
        for(EntityID id : changedEntities){
        	StandardEntity entity = this.worldInfo.getEntity(id);
        	if(entity instanceof Road) {
        		Road road = (Road)entity;
        		if(!road.isBlockadesDefined() || road.getBlockades().isEmpty()) {
        			clearedRoads.add(id);
        		}
        	}
        }
        
        // 自分の視界情報を送信
        for(EntityID id : changedEntities) {
        	StandardEntity standardEntity = this.worldInfo.getEntity(id);
        	if(standardEntity instanceof Civilian) {
        		Civilian civilian = (Civilian)standardEntity;
        		StandardEntity position = this.worldInfo.getEntity(civilian.getPosition());
        		if(!(position instanceof Refuge)){
        			if(civilian.isHPDefined() && civilian.getHP() > 0){
        				if((civilian.isBuriednessDefined() && civilian.getBuriedness() > 0) ||
        						(civilian.isDamageDefined() && civilian.getDamage() > 0)){
        					MessageCivilian message = new MessageCivilian(isRadio, civilian);
        					if(isRadio && limitBandwidth - message.toByteArray().length + 1 > 0){
        						messageManager.addMessage(new MessageCivilian(isRadio, civilian));
        						limitBandwidth -= message.toByteArray().length + 1;
        					}else{
        						isRadio = false;
        						if(limitVoice - message.toByteArray().length + 1 > 0 && limitVoiceCount > 0){
        							messageManager.addMessage(new MessageCivilian(isRadio, civilian));
        							limitVoice -= message.toByteArray().length + 1;
        							limitVoiceCount--;
        						}
        					}
        				}
        			}
        		}
        	}else if(standardEntity instanceof Building){
        		Building building = (Building)standardEntity;
				if (building.isOnFire()) { // 燃えている建物だけ送る
					CommunicationMessage message = new MessageBuilding(isRadio, building);
					if (isRadio && limitBandwidth - message.toByteArray().length + 1 > 0) {
						messageManager.addMessage(new MessageBuilding(isRadio, building));
						limitBandwidth -= message.toByteArray().length + 1;
					} else {
						isRadio = false;
						if (limitVoice - message.toByteArray().length + 1 > 0 && limitVoiceCount > 0) {
							messageManager.addMessage(new MessageBuilding(isRadio, building));
							limitVoice -= message.toByteArray().length + 1;
							limitVoiceCount--;
						}
					}
				}
        	}
        }
        
    	//他Agentが送信した情報を受信して処理
        for(CommunicationMessage message : messageManager.getReceivedMessageList()) {
        	if(message instanceof MessageCivilian) {
        		MessageCivilian mc = (MessageCivilian) message;
        		if(!changedEntities.contains(mc.getAgentID())){
        			MessageUtil.reflectMessage(this.worldInfo, mc);
        		}
        	} else if(message instanceof MessageBuilding){
        		MessageBuilding mb = (MessageBuilding)message;
        		if(!changedEntities.contains(mb.getBuildingID())) {
        			MessageUtil.reflectMessage(this.worldInfo, mb);
        		}
        	} else if(message instanceof MessageAmbulanceTeam) {
        		MessageAmbulanceTeam mat = (MessageAmbulanceTeam)message;
        		if(!changedEntities.contains(mat.getAgentID())) {
        			MessageUtil.reflectMessage(this.worldInfo, mat);
        			prioritizeByMessage(mat);
        		}
        		if(mat.getAction() == MessageAmbulanceTeam.ACTION_RESCUE || mat.getAction() == MessageAmbulanceTeam.ACTION_LOAD){
        			if(mat.getAgentID() != this.agentInfo.me().getID()){
        				activeHumans.add((Human)this.worldInfo.getEntity(mat.getTargetID()));
        			}
        		}
        	} else if(message instanceof MessageFireBrigade) {
        		MessageFireBrigade mfb = (MessageFireBrigade) message;
        		if(!changedEntities.contains(mfb.getAgentID())) {
        			MessageUtil.reflectMessage(this.worldInfo, mfb);
        			prioritizeByMessage(mfb);
        		}
        	} else if(message instanceof MessagePoliceForce) {
        		MessagePoliceForce mpf = (MessagePoliceForce) message;
        		if(!changedEntities.contains(mpf.getAgentID())) {
        			MessageUtil.reflectMessage(this.worldInfo, mpf);
        		    prioritizeByMessage(mpf);
        		}
        	} else if(message instanceof CommandPolice) {
                prioritizeByMessage((CommandPolice)message);
            } else if(message instanceof MessageRoad) {
                prioritizeByMessage((MessageRoad)message, changedEntities);
            }
        }
    	
        this.buriedHumans.clear();
        int clusterIndex = clustering.getClusterIndex(this.agentInfo.me().getID());
        Collection<StandardEntity> elements = clustering.getClusterEntities(clusterIndex);
        
        for (StandardEntity next : this.worldInfo.getEntitiesOfType(CIVILIAN,FIRE_BRIGADE, POLICE_FORCE)) {
        	Human h = (Human) next;
        	StandardEntity positionEntity = this.worldInfo.getPosition(h);
        	if(positionEntity != null && positionEntity instanceof Building) {
        		if (elements.contains(positionEntity) || elements.contains(h)) {
        			if (h.isHPDefined() && h.getHP() > 0) {
        				if ((h.isBuriednessDefined() && h.getBuriedness() > 0) ||
        						(h.isDamageDefined() && h.getDamage() > 0) &&
        						!(positionEntity instanceof Refuge) &&
        						!(positionEntity instanceof AmbulanceTeam)) {
        					this.buriedHumans.add(h);
        				}
        			}
        		}
        	}
        }
        this.buriedHumans.removeAll(activeHumans);
    	
    	// キーにroadID，値に優先度をもったMapを優先度0で初期化する    	
    	initPriorities();
    	// 瓦礫に挟まれているエージェントのいるroadの優先度を上げる
    	prioritizeByAgents();
    	//交差点の優先度を上げる
    	prioritizeByIntersections();
    	// 自分のクラスター内の優先度を上げる
    	prioritizeByCluster();
    	// refugeのエントランスの優先度を上げる
    	prioritizeByRefuge();
    	//建物の中にいる人数が多ければ優先度を上げる
    	prioritizeByCrowdMember();
    	// 燃え尽きた建物の半径10000mmのroadの優先度を下げる
    	deprioritizeByBuilding();
    	// 瓦礫が無いroadの優先度をhashmapから省く(-1にする)
    	deprioritizeByClearedRoads();
    	
    	//System.out.println("cleared Roads " + this.agentInfo.me().getID());
    	//clearedRoads.stream().forEach(System.out::println);
    	
    	// 余震が起こったか判断する 精度は微妙
    	if(aftershockOccured(changedEntities)){
    		//System.out.println("aftershock");
    		clearedRoads.clear();
    	}
    	updatePreviousBlockadesNumber(changedEntities);
    	
        if(this.result != null) {
        	if(this.agentInfo.getPosition().equals(this.result)) {
                StandardEntity entity = this.worldInfo.getEntity(this.result);
                if(entity instanceof Building) {
                    this.result = null;
                }else if(entity instanceof Road) {
                    Road road = (Road)entity;
                    if(!road.isBlockadesDefined() || road.getBlockades().isEmpty()) {
                    	//System.out.println("No blockades");
                    	this.result = null;
                    }
                }
            }
        }
        /*
        if(this.agentInfo.me().getID().getValue() == 1100051817){
	        System.out.println("■" + agentInfo.me().getID() + "  target : " + this.result);
	        targetPriorities.entrySet().stream()
	        	.filter(e -> e.getValue() > 0)
		        .sorted(java.util.Collections.reverseOrder(java.util.Map.Entry.comparingByValue()))
		        .limit(5)
		        .forEach(e -> {
		        	System.out.println("Road = " + e.getKey() + " : " + e.getValue());
		        });
        }
        */
        //System.out.println("ID : " + agentInfo.me().getID() + "  帯域残り : " + limitBandwidth + "  ボイス残り : " + limitVoice);

        return this;
    }
    
    boolean aftershockOccured(Set<EntityID> changedEntities){
    	for(EntityID id : changedEntities){
    		if(previousBlockadesNumber.containsKey(id)){
    			if(previousBlockadesNumber.get(id) > 0 && // 情報のないroadは瓦礫0とみなされるため0より多いもので比べる
    				((Road)this.worldInfo.getEntity(id)).getBlockades().size() > previousBlockadesNumber.get(id)){
    				return true; // 余震あり
    			}
    		}
    	}
    	return false; // 余震なし
    }

    void updatePreviousBlockadesNumber(Set<EntityID> changedEntities){
    	previousBlockadesNumber.clear();
    	for(EntityID id : changedEntities){
    		StandardEntity standardEntity = this.worldInfo.getEntity(id);
    		if(standardEntity instanceof Road){
    			Road road = (Road)standardEntity;
    			if(road.isBlockadesDefined()){
    				previousBlockadesNumber.put(id, road.getBlockades().size());
    			}else{
    				previousBlockadesNumber.put(id, 0);
    			}
    		}
    	}
    }
    
    // prioritize
    void prioritizeValues(EntityID id, double value){
	    if(targetPriorities.containsKey(id)){
			double tempValue = targetPriorities.get(id);
		    targetPriorities.put(id, tempValue + value);
		}
    }
    
    private void prioritizeByAgents() {
    	for(StandardEntity agent : this.worldInfo.getEntitiesOfType(FIRE_BRIGADE, AMBULANCE_TEAM)){
    		Human human = (Human)agent;
    		StandardEntity positionEntity = this.worldInfo.getPosition(human);
    		if(positionEntity instanceof Road){
    		    Road road = (Road)positionEntity;
    		    if(road.isBlockadesDefined() && road.getBlockades().size() > 0){
    		    	for(Blockade blockade : this.worldInfo.getBlockades(road)){
    		            if(!blockade.isApexesDefined()){
    		                continue;
    		            }
    		            if(this.isInBlockade(human)){
    		            	EntityID keyID = road.getID();
    		            	
    		            	if(agent instanceof AmbulanceTeam){
    		            		prioritizeValues(keyID, 1);
    		            	}
    		            	if(agent instanceof FireBrigade){
    		            		prioritizeValues(keyID, 3);
    		            	}
    		            	
    		            	break;
    		            }
    		        }
    		    }
    		}
	    }
   }
	
	private void prioritizeByIntersections(){
		intersections.stream()
			.forEach(id -> prioritizeValues(id, 1));
	}
	   
    private void deprioritizeByClearedRoads() {
    	for(EntityID id : clearedRoads){
    		if(targetPriorities.keySet().contains(id)){
    			targetPriorities.put(id, -1.0);
    		}
    	}
	}
    
    private void prioritizeByCluster() {
    	int index = this.clustering.getClusterIndex(this.agentInfo.me().getID());
    	this.clustering.getClusterEntities(index).stream()
    		.filter(se -> se instanceof Road)
    		.map(se -> se.getID())
    		.forEach(id -> prioritizeValues(id, 2));
	}
    
    private void prioritizeByRefuge() {
    	this.worldInfo.getEntitiesOfType(REFUGE).stream()
    		.map(se -> ((Area)se).getNeighbours())
    		.forEach(set -> {
	    		set.stream()
	    			.filter(ne -> worldInfo.getEntity(ne) instanceof Road)
	    			.forEach(id -> prioritizeValues(id, 2));
    		});
	}
    
    private void deprioritizeByBuilding() {
    	List<EntityID> burntList = this.worldInfo.getEntitiesOfType(BUILDING).stream()
    		.filter(se -> ((Building)se).getFierynessEnum() == Fieryness.BURNT_OUT)
    		.map(se -> se.getID())
    		.collect(Collectors.toList());
    	
    	for (EntityID idBurnt : burntList) {
    		worldInfo.getObjectIDsInRange(idBurnt, 10000).stream()
    			.filter(id -> this.worldInfo.getEntity(id) instanceof Road)
    			.forEach(id -> prioritizeValues(id, -2));
    	}
	}
    
    private void prioritizeByMessage(MessageRoad messageRoad, Collection<EntityID> changedEntities) {
        // 瓦礫がなかったRoadに瓦礫がある情報が来た場合受け取らない
        // 見てないRoadは瓦礫ないのと同じならここは変える必要がある
        if(!changedEntities.contains(messageRoad.getRoadID())){
	        if(messageRoad.isBlockadeDefined()){
	        	if(!changedEntities.contains(messageRoad.getBlockadeID()) &&
	        		((Road)this.worldInfo.getEntity(messageRoad.getRoadID())).isBlockadesDefined()){
	        		MessageUtil.reflectMessage(this.worldInfo, messageRoad);
	        	}
	        }else{
	        	MessageUtil.reflectMessage(this.worldInfo, messageRoad);
	        }
	        
	        // 通行可能なので優先度を-1.0に設定する
	        if(messageRoad.isPassable() && targetPriorities.containsKey(messageRoad.getRoadID())) {
    			targetPriorities.put(messageRoad.getRoadID(), -1.0);
    			//System.out.println(messageRoad.getRoadID() + " is Passable");
    			// clearedSetに追加
            }
        }
    }
    
    private void prioritizeByMessage(MessageAmbulanceTeam messageAmbulanceTeam) {
        if(messageAmbulanceTeam.getPosition() == null) {
            return;
        }
    	// getAction()がACTION_RESCUEまたはACTION_LOADの場合，その建物のエントランスは優先度を下げる
        if(messageAmbulanceTeam.getAction() == MessageAmbulanceTeam.ACTION_RESCUE ||
       		messageAmbulanceTeam.getAction() == MessageAmbulanceTeam.ACTION_LOAD) {
            StandardEntity position = this.worldInfo.getEntity(messageAmbulanceTeam.getPosition());
            if(position != null && position instanceof Building) {
            	((Building)position).getNeighbours().stream()
            		.forEach(id -> prioritizeValues(id, -2));
            }
        }
        // getAction()がACTION_MOVEの場合
    	// 1.targetが建物ならば，そのエントランスは優先度を上げる
        // 2.targetがhumanであり，そこが建物ならば，そのエントランスは優先度を上げる
        if(messageAmbulanceTeam.getAction() == MessageAmbulanceTeam.ACTION_MOVE) {
        	if(messageAmbulanceTeam.getTargetID() == null) {
                return;
            }
            StandardEntity target = this.worldInfo.getEntity(messageAmbulanceTeam.getTargetID());
            if(target instanceof Building) {
            	((Building)target).getNeighbours().stream()
	        		.forEach(id -> prioritizeValues(id, 2));
            } else if(target instanceof Human) {
                Human human = (Human)target;
                if(human.isPositionDefined()) {
                    StandardEntity position = this.worldInfo.getPosition(human);
                    if(position instanceof Building) {
                    	((Building)position).getNeighbours().stream()
	                		.forEach(id -> prioritizeValues(id, 2));
                    }
                }
            }
        }
    }
    
    private void prioritizeByMessage(MessageFireBrigade messageFireBrigade) {
        if(messageFireBrigade.getTargetID() == null) {
            return;
        }
        // getAction()がACTION_MOVEの場合
    	// 1.targetが建物ならば，そのエントランスは優先度を上げる
        // 2.targetがroadならば，そのroadの優先度を上げる
        if(messageFireBrigade.getAction() == MessageFireBrigade.ACTION_MOVE) {
        	if(messageFireBrigade.getTargetID() == null) {
                return;
            }
            StandardEntity target = this.worldInfo.getEntity(messageFireBrigade.getTargetID());
            if(target instanceof Road){
            	prioritizeValues(target.getID(), 3);
            }else if(target instanceof Building) {
            	((Building)target).getNeighbours().stream()
	        		.forEach(id -> prioritizeValues(id, 3));
            }
        }
        // getAction()がACTION_REFILLの場合（補給地点に辿り着いているので）
    	// 1.targetが建物ならば，そのエントランスは優先度を下げる
        // 2.targetがroadならば，そのroadの優先度を下げる
        if(messageFireBrigade.getAction() == MessageFireBrigade.ACTION_REFILL) {
            StandardEntity target = this.worldInfo.getEntity(messageFireBrigade.getTargetID());
            if(target instanceof Building) {
            	((Building)target).getNeighbours().stream()
	        		.forEach(id -> prioritizeValues(id, -2));
            } else if(target.getStandardURN() == HYDRANT) {
                prioritizeValues(target.getID(), 1);
            }
        }
    }
    
    private void prioritizeByMessage(MessagePoliceForce messagePoliceForce) {
        if(messagePoliceForce.getAction() == MessagePoliceForce.ACTION_CLEAR) {
            if(messagePoliceForce.getAgentID().getValue() != this.agentInfo.getID().getValue()) {
                if (messagePoliceForce.isTargetDefined()) {
                    EntityID targetID = messagePoliceForce.getTargetID();
                    if(targetID == null) {
                        return;
                    }
                    StandardEntity entity = this.worldInfo.getEntity(targetID);
                    if (entity == null) {
                        return;
                    }

                    if (entity instanceof Area) {
                    	if (entity instanceof Road){
                    		prioritizeValues(targetID, -3);
                    	}
                        if(this.result != null && this.result.getValue() == targetID.getValue()) {
                        	// どちらか一方のresultをnullにする
                            if(this.agentInfo.getID().getValue() < messagePoliceForce.getAgentID().getValue()) {
                                this.result = null;
                            }
                        }
                    } else if (entity.getStandardURN() == BLOCKADE) {
                        EntityID position = ((Blockade) entity).getPosition();
	                    prioritizeValues(position, -3);
                        if(this.result != null && this.result.getValue() == position.getValue()) {
                        	// どちらか一方のresultをnullにする
                            if(this.agentInfo.getID().getValue() < messagePoliceForce.getAgentID().getValue()) {
                                this.result = null;
                            }
                        }
                    }

                }
            }
        }
    }

    private void prioritizeByMessage(CommandPolice commandPolice) {
        boolean flag = false;
        if(commandPolice.isToIDDefined() && this.agentInfo.getID().getValue() == commandPolice.getToID().getValue()) {
            flag = true;
        } else if(commandPolice.isBroadcast()) {
            flag = true;
        }
        if(flag && commandPolice.getAction() == CommandPolice.ACTION_CLEAR) {
            if(commandPolice.getTargetID() == null) {
                return;
            }
            StandardEntity target = this.worldInfo.getEntity(commandPolice.getTargetID());
            if(target instanceof Road) {         	
            	prioritizeValues(target.getID(), 3);
            } else if(target.getStandardURN() == BLOCKADE) {
                Blockade blockade = (Blockade)target;
                if(blockade.isPositionDefined()) {
                	prioritizeValues(blockade.getPosition(), 3);
                }
            }
        }
    }
    
    private Map<Building, Integer> crowdPosition(List<Human> list) {  
		Map<Building, Integer> pMap = new HashMap<Building, Integer>();  
		int k=0;  
		for(Human human : list){//サーバーから埋まっている人を持ってきて建物に何人いるか判定  
			StandardEntity pos = worldInfo.getEntity(human.getPosition()); 
			if(pos instanceof Building){
				Building posBuilding=(Building)pos;  
				if (pMap.containsKey(posBuilding)){//buildingの中にいるかを判定  
					k=pMap.get(posBuilding)+1;  
				}  
				pMap.put(posBuilding, k);
			}  
		}
		return pMap;
	}
    
    private void prioritizeByCrowdMember(){
    	Map<Building, Integer> crowdMember =  this.crowdPosition(this.buriedHumans);
 
    	for(Human human : this.buriedHumans){
			StandardEntity se = this.worldInfo.getEntity(human.getPosition());
			if (se instanceof Building) {
				Building building = (Building)se;
				if(!building.isOnFire()){
					int numHuman = crowdMember.containsKey(building) ? crowdMember.get(building) : 0;
					if(numHuman > 1){
						for(EntityID id : building.getNeighbours()){
							prioritizeValues(id, 1);
				        }
					}
				}
			}
		}
    }
    
    
    
    // Init
	private void communicationInitialize() {
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
    
    private void initializeSets() {
    	this.worldInfo.getEntitiesOfType(ROAD).stream()
	    	.filter(se -> ((Road)se).getNeighbours().stream().filter(nb -> this.worldInfo.getEntity(nb) instanceof Road).count() == 4)
	    	.forEach(se -> {
	    		((Area)se).getNeighbours().stream()
	    			.map(id -> this.worldInfo.getEntity(id))
	    			.filter(nse -> nse instanceof Road)
	    			.forEach(road -> {
	    				for(EntityID nextID : ((Road)road).getNeighbours()){
	    					StandardEntity nextNeighbour = this.worldInfo.getEntity(nextID);
	    					if(nextNeighbour instanceof Road && 
	    						((Road)nextNeighbour).getNeighbours().stream().filter(nnse -> this.worldInfo.getEntity(nnse) instanceof Road).count() == 2){
	    						this.intersections.add(road.getID());
	    					}
	    				}
	    			});
	    	});
    	// 過密な交差点を省く
    	Set<EntityID> removeSet = new HashSet<>();
    	for(EntityID id_a : intersections){
    		Collection<EntityID> set = this.worldInfo.getObjectIDsInRange(id_a, 50000);
    		intersections.stream()
    			.filter(id_b -> id_b != id_a && set.contains(id_b))
    			.forEach(id_b -> {
    				removeSet.add(id_a);
    				removeSet.add(id_b);
    			});
    	}
    	intersections.removeAll(removeSet);
	}
    
    private void initPriorities(){
    	worldInfo.getEntitiesOfType(ROAD).stream()
    		.forEach(se -> targetPriorities.put(se.getID(), 0.0));
    }
    
    // Utils
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
