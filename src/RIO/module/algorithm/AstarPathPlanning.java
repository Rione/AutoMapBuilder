package RIO.module.algorithm;

import adf.agent.communication.MessageManager;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.module.algorithm.PathPlanning;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.awt.Polygon;
import java.util.*;

import java.awt.Shape;

/*
 * 作成者:林光希
 * 作成日時:2016/12/17
 * 使用Agent:AT,FB,PF
 * Astarアルゴリズムの実コストにclearcostを使用
 */
public class AstarPathPlanning extends PathPlanning {
    private EntityID from;
    private Collection<EntityID> targets;
    private List<EntityID> result;
    private List<EntityID> extendedBlockadeRoads;

    public AstarPathPlanning(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);
        extendedBlockadeRoads = new ArrayList<>();
    }

    @Override
    public List<EntityID> getResult() {
        return this.result;
    }

    @Override
    public PathPlanning setFrom(EntityID id) {
        this.from = id;
        return this;
    }

    @Override
    public PathPlanning setDestination(Collection<EntityID> targets) {
			this.targets = targets;
			return this;
		}

    @Override
    public PathPlanning updateInfo(MessageManager messageManager) {
        super.updateInfo(messageManager);
        extendedBlockadeRoads.clear();
        setExtendedBlockadeRoads();
        return this;
    }
    
  //西田孝典
    private Void setExtendedBlockadeRoads(){
    	for(EntityID en:this.worldInfo.getChanged().getChangedEntities()) {
    		StandardEntity standardEntity = this.worldInfo.getEntity(en);
    		if(standardEntity instanceof Human) {
    			Human human =(Human)standardEntity;
    			if(isInExtendedBlockade(human)) {
    				extendedBlockadeRoads.add(human.getPosition());
    			}
    		}
    	}
    	return null;
    }
    
    private boolean isInExtendedBlockade(Human human) {
        if(!human.isXDefined() || !human.isXDefined()) return false;
        int agentX = human.getX();
        int agentY = human.getY();
        StandardEntity positionEntity = this.worldInfo.getPosition(human);
        if(positionEntity instanceof Road){
            Road road = (Road)positionEntity;
            if(road.isBlockadesDefined() && road.getBlockades().size() > 0){
                for(Blockade blockade : worldInfo.getBlockades(road)){
                    Shape extendedShape = getExtendedShape(blockade);
                    if(extendedShape != null && extendedShape.contains(agentX, agentY)){
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
	private Shape getExtendedShape(Blockade blockade) {
		Shape shape = null;
        if (shape == null) {
            int[] allApexes = blockade.getApexes();
            int count = allApexes.length / 2;
            int[] xs = new int[count];
            int[] ys = new int[count];
            double centerX = 0;
            double centerY = 0;
            for (int i = 0; i < count; ++i) {
                xs[i] = allApexes[i * 2];
                ys[i] = allApexes[i * 2 + 1];
                centerX += xs[i];
                centerY += ys[i];
            }
            centerX /= count;
            centerY /= count;
            for (int i = 0; i < count; ++i) {
            	// 重心から頂点へのベクトル
            	double vectorX = xs[i] - centerX;
            	double vectorY = ys[i] - centerY; 	
            	double magnitude = Math.sqrt(vectorX * vectorX + vectorY * vectorY); // ベクトルの大きさ
            	// 重心から頂点への大きさ2のベクトルを頂点に足して四捨五入
            	xs[i] += (vectorX / magnitude) * 2 + 0.5;
            	ys[i] += (vectorY / magnitude) * 2 + 0.5;
             }
            shape = new Polygon(xs, ys, count);
        }
        return shape;
    }
    
    @Override
    public PathPlanning precompute(PrecomputeData precomputeData) {
        super.precompute(precomputeData);
        return this;
    }

    @Override
    public PathPlanning resume(PrecomputeData precomputeData) {
        super.resume(precomputeData);
        return this;
    }

    @Override
    public PathPlanning preparate() {
        super.preparate();
        return this;
    }

    @Override
    public PathPlanning calc() {
        List<EntityID> open = new LinkedList<>();
        List<EntityID> close = new LinkedList<>();
        Map<EntityID, Node> nodeMap = new HashMap<>();
     
        open.add(this.from);
        nodeMap.put(this.from, new Node(null, this.from));
        close.clear();
     
        while(true){
        	//openlistが空なら探索失敗
            if(open.isEmpty()){
                this.result = null;
                return this;
            }
     
            //openlistからスコアが最も小さなnodeを取り出す
            Node n = null;
            for(EntityID id : open){
                Node node = nodeMap.get(id);
     
                if(n == null){
                    n = node;
                }else if(node.getScore() < n.getScore()){
                    n = node;
                }
            }
     
            //取り出したnodeが目的地なら探索終了. 親nodeを辿りpathを生成する
            if (isGoal(n.getID(), this.targets)){
                List<EntityID> path = new LinkedList<>();
                while(n != null){
                    path.add(0, n.getID());
                    n = nodeMap.get(n.getParent());
                }    
                this.result = path;
                return this;
            }
            
            //取り出したnodeが目的地でなければcloselistに移す
            open.remove(n.getID());
            close.add(n.getID());
            
            //取り出したnodeのneighborのスコアを計算する
            List<EntityID> neighbours = n.getNeighbor();
            for (EntityID neighbour : neighbours){
                Node m = new Node(n, neighbour);  
                /*
                 * open,closeどちらにも含まれない場合はopenに追加
                 * openに含まれていて新しいスコアが元のスコアより小さい場合は新しいスコアに更新
                 * closeに含まれていて新しいスコアが元のスコアより小さい場合は新しいスコアに更新してopenに移す
                 */
                if(!open.contains(neighbour) && !close.contains(neighbour)){
                    open.add(m.getID());
                    nodeMap.put(neighbour, m);
                }else if(open.contains(neighbour) && m.getScore() < nodeMap.get(neighbour).getScore()){
                    nodeMap.put(neighbour, m);
                }else if(close.contains(neighbour) && m.getScore() < nodeMap.get(neighbour).getScore()){
                    nodeMap.put(neighbour, m);
                    close.remove(m.getID());
                    open.add(m.getID());
                }
            }
        }
    } 

    private boolean isGoal(EntityID e, Collection<EntityID> targets) {
        return targets.contains(e);
    }
    
    //pathplanningに使用するNodeのクラス
    private class Node {
        private EntityID id;
        private EntityID parent;
        private List<EntityID> neighbor; 
        private double cost;
        private double heuristic;
     
        public Node(Node parent, EntityID id) {
            this.id = id;    
            if(parent == null){
                this.cost = 0;
                this.parent = null;
            }else{
                this.parent = parent.getID();
                this.cost = parent.getCost() + worldInfo.getDistance(parent.getID(), id) + getClearCost(id);
            }
            this.neighbor = ((Area)worldInfo.getEntity(id)).getNeighbours();
            this.neighbor.removeAll(extendedBlockadeRoads);//引っかる瓦礫がある道を候補から除く
            if(targets.size() == 0){
            	this.heuristic = 0;
            }else{
                this.heuristic = worldInfo.getDistance(id, targets.toArray(new EntityID[targets.size()])[0]);
            }
        }
     
        private EntityID getID() {
            return id;
        }
     
        private double getCost() {
            return cost;
        }
     
        private double getScore() {
            return cost + heuristic;
        }
     
        private EntityID getParent() {
            return this.parent;
        }
        
        private List<EntityID> getNeighbor() {
			return this.neighbor;
		}

        private double getClearCost(EntityID id){
        	StandardEntity standardEntity = worldInfo.getEntity(id);
        	//StandardEntity me = agentInfo.me();
        	if(standardEntity instanceof Road){
        		Road road = (Road)standardEntity;
        		if(road.isBlockadesDefined() && road.getBlockades().size() > 0){
        			double cost = 0;
        			for(Blockade blockade : worldInfo.getBlockades(road)){
        				if(blockade.isRepairCostDefined()){
        					cost += blockade.getRepairCost();
        				}
        			}
        			return cost;
        		}else{
        			return 0;
        		}
        	}else{
        		return 0;
        	}
        }
    }
}
