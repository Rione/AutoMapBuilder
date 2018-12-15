package RIO.extaction;

import adf.agent.action.Action;
import adf.agent.action.common.ActionMove;
import adf.agent.action.police.ActionClear;
import adf.agent.communication.MessageManager;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.extaction.ExtAction;
import adf.component.module.algorithm.Clustering;
import adf.component.module.algorithm.PathPlanning;
//import adf.tools.Measurement;
//import adf.tools.R_Exporter;
//import adf.tools.R_ClassURN.drawOption;
import rescuecore2.config.NoSuchConfigOptionException;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import static rescuecore2.standard.entities.StandardEntityURN.BUILDING;
import static rescuecore2.standard.entities.StandardEntityURN.REFUGE;

import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.math3.analysis.function.Sqrt;

import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import java.time.LocalTime;

public class RIOActionExtClear extends ExtAction {
	private PathPlanning pathPlanning;

	
    // calcSearch用
    private Clustering clustering;
    private ArrayList<Integer> movedTime;
    private boolean stopped;
    private ArrayList<EntityID> unsearchedBuildingIDs;
    private int clusterIndex;
    private int changeClusterCycle;
    protected Random random;
    private ArrayList<Point2D> previousLocations;
    private ArrayList<List<EntityID>> previousPaths;
	
	
	private int clearDistance;

	private int forcedMove;
	private int thresholdRest;
	private int kernelTime;

	final private EntityID firstPosition;//シミュレーション開始時点で突っ立ってるEntity
	private int errorCount;//移動しないエラーを吐いた数（いまのとこ数えてない）

	private EntityID target;
	private Map<EntityID, Set<Point2D>> movePointCache;
	private int oldClearX;
	private int oldClearY;
	private int count;

	private HashMap<EntityID, java.awt.geom.Area> forbiddenAreas=new HashMap<>();
	//private HashMap<EntityID, java.awt.geom.Area> safetyAreas=new HashMap<>();
	private HashMap<EntityID, List<Point2D>> safetyAreaApexes=new HashMap<>();
	private boolean calcSwitch=false;
	//private R_Exporter exporter=new R_Exporter();

	private Point2D oldTargetPoint=null;
	private Point2D oldAgentPoint=null;
	private Point2D beforeModificationPoint=null;
	private Point2D nowAgentPoint=null;
	private int emergencyAction=0;
	private int oldMoveX=0;
	private int oldMoveY=0;
	private int moveLimiteDistance=20000;
	private int forcedDistance=500;


	private HashMap<Edge, Edge> pairSymmetryEdge=new HashMap<>();		//向かい合った辺のペア

	private Point2D targetApex=null;							//目的頂点
	private boolean targetApex_isReachable=false;				//目的頂点に到達可能か
	private Point2D modificationTargetApex=null;				//目的頂点に最も近い同閉鎖エリア内頂点(瓦礫で分断されていない)
	private boolean modificationTargetApex_isReachable=false;	//同閉鎖エリア内頂点に到達可能か（直線上に瓦礫があるかないか）
	private double modificationTargetApexDistance=0;			//同閉鎖エリア内頂点までの距離(近いならClearするため)

	/*パスの次のエッジを取る
	 * エッジのエージェントに近い側の頂点を取る
	 * 最も近い同エリア内の理論上到達可能エリアを取る
	 *
	 * 案１（エリアは進入禁止エリアを除く場合と除かない場合があることを前提としている）
	 * その点とエージェントを結んで半径が距離、角度は点とエージェントの直線を基線に左右それぞれ10〜45度前後
	 * このエリアと理論上到達可能点を含むエリアをintersectし、エージェントと理論上到達可能点が同閉鎖パスにあるかを判定
	 * 同閉鎖パス内にあった時、エージェントと理論上到達可能点を結んだ線と同閉鎖パスの頂点の距離を調べる(垂直二等分線)
	 * 最も遠かった点の頂点に向かう。　こうすれば下手な溝には引っかからないはず
	 * 案２
	 * 正面180度のエリアをとり目的のエッジの中点とそれぞれのBlockadeの頂点のなす角を調べる
	 *
	 */
	private java.awt.geom.Area isArea=new java.awt.geom.Area();
	private Point2D agentPoint;

	private Point2D isPoint;


	private Point2D targetEntityPoint=null;

	private actionURN[] oldAction=new actionURN[4];
	private int oldX[]=new int[4];
	private int oldY[]=new int[4];
	private int oldTargetX[]=new int[4];
	private int oldTargetY[]=new int[4];

	private actionURN action;
	Point2D targetPoint;
	private int recoveryX;
	private int recoveryY;


	public enum actionURN{
		actionRest(0),
		actionMove(1),
		actionClear(2);

		private final int number;

		private actionURN(int number) {
			this.number=number;
		}
		public int getID() {
			return number;
		}
	}

	public RIOActionExtClear(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
		super(ai, wi, si, moduleManager, developData);
		this.clearDistance = si.getClearRepairDistance();
		this.forcedMove = developData.getInteger("ActionExtClear.forcedMove", 3);
		this.thresholdRest = developData.getInteger("ActionExtClear.rest", 100);

		this.firstPosition = ai.getPositionArea().getID();
		this.errorCount = 0;
		this.target = null;
		this.movePointCache = new HashMap<>();
		this.oldClearX = 0;
		this.oldClearY = 0;
		this.count = 0;

		switch  (si.getMode()) {
		case PRECOMPUTATION_PHASE:
			this.pathPlanning = moduleManager.getModule("RIOActionExtClear.PathPlanning", "RIO.module.algorithm.AstarPathPlanningPolice");
			break;
		case PRECOMPUTED:
			this.pathPlanning = moduleManager.getModule("RIOActionExtClear.PathPlanning", "RIO.module.algorithm.AstarPathPlanningPolice");
			break;
		case NON_PRECOMPUTE:
			this.pathPlanning = moduleManager.getModule("RIOActionExtClear.PathPlanning", "RIO.module.algorithm.AstarPathPlanningPolice");
			break;
		}
		
		// calcSearch用
        this.clustering = moduleManager.getModule("ActionTransport.Clustering.Ambulance", "RIO.module.algorithm.RioneKmeansPP");
        unsearchedBuildingIDs = new ArrayList<>();
        movedTime = new ArrayList<>();
        this.changeClusterCycle = 5;
        this.clusterIndex = 0;
        this.random = new Random();
        this.stopped = false;
        this.previousLocations = new ArrayList<>();
        this.previousPaths = new ArrayList<>();
	}

	@Override
	public ExtAction precompute(PrecomputeData precomputeData) {
		super.precompute(precomputeData);
		if(this.getCountPrecompute() >= 2) {
			return this;
		}
		this.pathPlanning.precompute(precomputeData);
		try {
			this.kernelTime = this.scenarioInfo.getKernelTimesteps();
		}catch (NoSuchConfigOptionException e) {
			this.kernelTime = -1;
		}
		return this;
	}

	@Override
	public ExtAction resume(PrecomputeData precomputeData) {
		super.resume(precomputeData);
		if(this.getCountResume() >= 2) {
			return this;
		}
		this.pathPlanning.resume(precomputeData);
		try {
			this.kernelTime = this.scenarioInfo.getKernelTimesteps();
		}catch (NoSuchConfigOptionException e) {
			this.kernelTime = -1;
		}
		return this;
	}

	@Override
	public ExtAction preparate() {
		super.preparate();
		if(this.getCountPreparate() >= 2) {
			return this;
		}
		this.pathPlanning.preparate();
		try {
			this.kernelTime = this.scenarioInfo.getKernelTimesteps();
		}catch (NoSuchConfigOptionException e) {
			this.kernelTime = -1;
		}
		return this;
	}

	@Override
	public ExtAction updateInfo(MessageManager messageManager){
		//exporter.checkTime();
		if(calcSwitch==false&&this.agentInfo.getID().equals((this.worldInfo.getEntitiesOfType(StandardEntityURN.POLICE_FORCE).stream().collect(Collectors.toList())).get(0).getID())) {
			this.calcSwitch=true;
		}
		super.updateInfo(messageManager);
		if(this.getCountUpdateInfo() >= 2) {
			return this;
		}
		this.pathPlanning.updateInfo(messageManager);
		return this;
	}

	@Override
	public ExtAction setTarget(EntityID target) {
		this.target = target;
		/*
		StandardEntity entity = this.worldInfo.getEntity(target);
		if(entity != null) {
			if (entity instanceof Road) {
				this.target = target;
			} else if (entity.getStandardURN().equals(StandardEntityURN.BLOCKADE)) {
				this.target = ((Blockade) entity).getPosition();
			} else if (entity instanceof Building) {
				this.target = target;
			}
		}*/
		return this;
	}
	//■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■
	//■■■■　calc系　■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■
	//■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■
	@Override
	public ExtAction calc() {
		//Measurement measurement=new Measurement();
		//measurement.start();
		//System.out.println(agentInfo.getID()+" → "+this.target);
		LocalTime T = LocalTime.now();
		PoliceForce agent=(PoliceForce)this.agentInfo.me();
		this.agentPoint=toPoint2D(agent.getX(), agent.getY());
		//exporter.exportLog_Circle(drawOption.fill, agentPoint.getX(), agentPoint.getY(), 3000000, 0, 255, 0, 255);
		this.result=null;
		this.action=null;
		this.targetEntityPoint=null;
		//System.out.println(this.target);
		/*
		if(errorCount>0){//エラー吐いたことある時用の初期位置への帰還処理
			this.target = this.firstPosition;
			StandardEntity standardEntity=this.worldInfo.getEntity(this.firstPosition);
			EntityID targetArea=null;
			if(standardEntity instanceof Area) {
				Area area=(Area)standardEntity;
				//exporter.exportLog_Line(agent.getX(), agent.getY(), area.getX(), area.getY(), 0, 255, 0, 255);
				targetArea=this.target;
				this.targetEntityPoint=calcOnTarget(area.getID());
				if(this.targetEntityPoint==null){
					this.targetEntityPoint=toPoint2D(area.getX(), area.getY());
				}
			}else if(standardEntity instanceof Blockade) {
				Blockade blockade=(Blockade)standardEntity;
				//exporter.exportLog_Line(agent.getX(), agent.getY(), blockade.getX(), blockade.getY(), 0, 255, 0, 255);
				targetArea=blockade.getPosition();
				this.targetEntityPoint=toPoint2D(blockade.getX(), blockade.getY());
			}else if(standardEntity instanceof Human) {
				Human human=(Human)standardEntity;
				if(human!=null&&human.isXDefined()&&human.isYDefined()){
					targetArea=human.getPosition();
					this.targetEntityPoint=toPoint2D(human.getX(), human.getY());
				}
			}
			if(targetArea!=null) {
				//Pathが生成できたら実際のアクションを考えさせる
				List<EntityID>path=calcPath(this.agentInfo.getPosition(), targetArea);
				//System.out.println("パス"+path);
				if(path!=null&&!path.isEmpty()){
					this.result=calcAction(path);
					//System.out.println(agentInfo.getID()+":::"+this.target+":  →  :"+this.result);
					registeredStamp(action, agent.getX(), agent.getY(), (int)this.targetPoint.getX(), (int)this.targetPoint.getY());
					if(isParalysis()){
						registeredReset();
						if(isNearly(recoveryX, recoveryY, (int)agent.getX(), (int)agent.getY(), 100)){
							//System.out.println(T.getHour()+":"+T.getMinute());//Logから観測できるようにLocalTimeをとってます
							//System.out.println(agentInfo.getID()+" 初期地点にも移動できません");
							errorCount = 0;
							//setTarget(firstPosition);
							this.result = new ActionMove(path);
						}else{
							this.result = calcClearAction(targetPoint);
						}
						recoveryX=(int)agent.getX();
						recoveryY=(int)agent.getY();
						registeredStamp(actionURN.actionClear, agent.getX(), agent.getY(), (int)this.targetPoint.getX(), (int)this.targetPoint.getY());
					}
				}
			}
		}*/
		if(this.target!=null) {
			//目標によってターゲットをエリアに変換するようにする
			StandardEntity standardEntity=this.worldInfo.getEntity(this.target);
			EntityID targetArea=null;
			if(standardEntity instanceof Area) {
				Area area=(Area)standardEntity;
				//exporter.exportLog_Line(agent.getX(), agent.getY(), area.getX(), area.getY(), 0, 255, 0, 255);
				targetArea=this.target;
				this.targetEntityPoint=calcOnTarget(area.getID());
				if(this.targetEntityPoint==null){
					this.targetEntityPoint=toPoint2D(area.getX(), area.getY());
				}
			}else if(standardEntity instanceof Blockade) {
				Blockade blockade=(Blockade)standardEntity;
				//exporter.exportLog_Line(agent.getX(), agent.getY(), blockade.getX(), blockade.getY(), 0, 255, 0, 255);
				targetArea=blockade.getPosition();
				this.targetEntityPoint=toPoint2D(blockade.getX(), blockade.getY());
			}else if(standardEntity instanceof Human) {
				Human human=(Human)standardEntity;
				if(human!=null&&human.isXDefined()&&human.isYDefined()){
					targetArea=human.getPosition();
					this.targetEntityPoint=toPoint2D(human.getX(), human.getY());
				}
			}
			if(targetArea!=null) {
				//Pathが生成できたら実際のアクションを考えさせる
				List<EntityID>path=calcPath(this.agentInfo.getPosition(), targetArea);
				//System.out.println("パス"+path);
				if(path!=null&&!path.isEmpty()){
					this.result=calcAction(path);
					//System.out.println(agentInfo.getID()+":::"+this.target+":  →  :"+this.result);
					registeredStamp(action, agent.getX(), agent.getY(), (int)this.targetPoint.getX(), (int)this.targetPoint.getY());
					if(isParalysis()){
						registeredReset();
						if(isNearly(recoveryX, recoveryY, (int)agent.getX(), (int)agent.getY(), 100)){
							//System.out.println(T.getHour()+":"+T.getMinute());//Logから観測できるようにLocalTimeをとってます
							//System.out.println(agentInfo.getID()+" 移動指示しても動かないポンコツ");
							//setTarget(firstPosition);
							//errorCount++;
							//this.result=new ActionMove(path);
							/*
							if(this.result == null)
								this.result = calcSearch();
							if(this.result == null)
								this.result=new ActionMove(path);
							*/
							System.out.println(path);
							Random rand = new Random();
							int randomNum = rand.nextInt(10);
							System.out.println("paralysis " + randomNum);
							if(randomNum >= 5){
								this.result = new ActionMove(path);
							}else
								this.result = calcClearAction(targetPoint);
						}else{
							this.result=calcClearAction(targetPoint);
						}
						recoveryX=(int)agent.getX();
						recoveryY=(int)agent.getY();
						registeredStamp(actionURN.actionClear, agent.getX(), agent.getY(), (int)this.targetPoint.getX(), (int)this.targetPoint.getY());
					}
				}
			}
		}
		//measurement.end();
		//measurement.printResult();
		//System.out.println(this.result);
		
		
		// 拡張した瓦礫に挟まってるエージェントがいるならそのエージェントを助ける
		helpAgentInExtendedBlockade();
		// 拡張した瓦礫に挟まってるエージェントがいるならそのエージェントの座標へ移動
		moveToAgentInExtendedBlockade();
		// 瓦礫に挟まってるエージェントがいるならそのエージェントを助ける
		helpAgentInBlockade();
		
		return this;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * 指定ポイント方向に向かってシナリオに定義されたClearDistanceにベクトルを再調整してActionClearを返す
	 * 座標x,yからも出せるように
	 *
	 * @param targetX, targetY
	 * @return ActionClear
	 * @author 岡島
	 */
	private Action calcClearAction(int targetX, int targetY) {
		Point2D targetPoint = new Point2D((double)targetX, (double)targetY);
		Vector2D targetVector = toVector2D(agentPoint, targetPoint).normalised().scale(this.scenarioInfo.getClearRepairDistance());
		targetPoint = toPoint2D(agentPoint, targetVector);
		this.targetPoint = targetPoint;
		int fixedTargetX=(int)targetPoint.getX();
		int fixedTargetY=(int)targetPoint.getY();
		//exporter.exportLog_Line(agentPoint, targetPoint, 0, 0, 255, 255);
		return new ActionClear(fixedTargetX, fixedTargetY);
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * 瓦礫に挟まってるエージェントがいるならそのエージェントを助ける関数
	 * @author 岡島
	 */
	private void helpAgentInBlockade() {
		Set<EntityID> changedEntities = this.worldInfo.getChanged().getChangedEntities();
		StandardEntity caughtAgent = changedEntities.stream()
				.map(id -> this.worldInfo.getEntity(id))
				.filter(se -> se instanceof AmbulanceTeam || se instanceof FireBrigade)
				.filter(se -> isInBlockade((Human)se))
				.findFirst().orElse(null);

		if(caughtAgent != null){
			// 5m以内のエージェントに挟まったエージェントがいるなら助ける 距離もう少し長くても良いか
			int distance = this.worldInfo.getDistance(this.agentInfo.me().getID(), caughtAgent.getID());
			//System.out.println(distance);
			if(0 < distance && distance < 5000){
				//System.out.println(this.agentInfo.me().getID() + " ActionClear to " + caughtAgent.getID() + " : " + distance);
				this.result = calcClearAction(((Human)caughtAgent).getX(), ((Human)caughtAgent).getY());
			} else if(caughtAgent != null){
				//System.out.println(this.agentInfo.me().getID() + " found " + caughtAgent.getID() + " : " + distance);
			}
		}
	}
	/**
	 * 拡張した瓦礫に挟まってるエージェントがいるならそのエージェントを助ける関数
	 * @author 岡島
	 */
	private void helpAgentInExtendedBlockade() {
		Set<EntityID> changedEntities = this.worldInfo.getChanged().getChangedEntities();
		StandardEntity caughtAgent = changedEntities.stream()
				.map(id -> this.worldInfo.getEntity(id))
				.filter(se -> se instanceof AmbulanceTeam || se instanceof FireBrigade)
				.filter(se -> isInExtendedBlockade((Human)se))
				.findFirst().orElse(null);

		if(caughtAgent != null){
			// 5m以内のエージェントに挟まったエージェントがいるなら助ける 距離もう少し長くても良いか
			int distance = this.worldInfo.getDistance(this.agentInfo.me().getID(), caughtAgent.getID());
			//System.out.println(distance);
			if(0 < distance && distance < 5000){
				//System.out.println(this.agentInfo.me().getID() + " ActionClear to (EXTEND) " + caughtAgent.getID() + " : " + distance);
				this.result = calcClearAction(((Human)caughtAgent).getX(), ((Human)caughtAgent).getY());
			}
		}
	}
	/**
	 * 拡張した瓦礫に挟まってるエージェントがいるならそのエージェントの座標に重なるように動く
	 * @author 岡島
	 */
	private void moveToAgentInExtendedBlockade() {
		Set<EntityID> changedEntities = this.worldInfo.getChanged().getChangedEntities();
		// 瓦礫内部にはいないが瓦礫に挟まったエージェント
		StandardEntity caughtAgentNotIn = changedEntities.stream()
				.map(id -> this.worldInfo.getEntity(id))
				.filter(se -> se instanceof AmbulanceTeam || se instanceof FireBrigade)
				.filter(se -> isInExtendedBlockade((Human)se))
				.filter(se -> !isInBlockade((Human)se))
				.findFirst().orElse(null);

		if(caughtAgentNotIn != null){
			// 10m以内のエージェントに挟まったエージェントがいるなら移動
			int distance = this.worldInfo.getDistance(this.agentInfo.me().getID(), caughtAgentNotIn.getID());
			//System.out.println(distance);
			
			if(1000 < distance && distance < 10000){ // 1mいないだとすでに重なっていると見なす
				int myPosX = ((Human)this.agentInfo.me()).getX();
				int myPosY = ((Human)this.agentInfo.me()).getY();
				int agentPosX = ((Human) caughtAgentNotIn).getX();
				int agentPosY = ((Human) caughtAgentNotIn).getY();
				
				// 移動先との間に瓦礫がある
				if(existsBlockade(myPosX, myPosY, agentPosX, agentPosY)){
					return;
				}
				//System.out.println(this.agentInfo.me().getID() + " ActionMove to " + caughtAgentNotIn.getID() + " : " + distance);
				List<EntityID> path = new ArrayList<EntityID>();
				EntityID startPosition = this.worldInfo.getPosition(this.agentInfo.me().getID()).getID();
				EntityID endPosition = this.worldInfo.getPosition((Human)caughtAgentNotIn).getID();
				path = calcPath(startPosition, endPosition);
				if(path != null)
					this.result = new ActionMove(path, agentPosX, agentPosY);
			}
		}
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * 二点間に瓦礫があるならtrueを返す関数(中点で考えるので距離が伸びると精度が落ちる)
	 * @param fromX fromY toX toY
	 * @return true or false
	 * @author 岡島
	 */
	private boolean existsBlockade(int fromX, int fromY, int toX, int toY) {
		Set<EntityID> changedEntities = this.worldInfo.getChanged().getChangedEntities();
		Set<Blockade> blockades = new HashSet<Blockade>();
		changedEntities.stream()
			.filter(id -> this.worldInfo.getEntity(id) instanceof Blockade)
			.forEach(id -> blockades.add((Blockade)this.worldInfo.getEntity(id)));
		
		int centerX = (fromX + toX) / 2;
		int centerY = (fromY + toY) / 2;
		
		if(blockades != null && !blockades.isEmpty()){
			for(Blockade blockade : blockades){
				if(blockade.getShape().contains(centerX, centerY)){
					return true;
				}
			}
		}
		return false;
	}
	/**
	 * 指定したHumanが瓦礫に挟まっているならtrueを返す関数
	 * @param human
	 * @return true or false
	 * @author 岡島
	 */
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
	/**
	 * 指定したHumanが拡張した瓦礫に挟まっているならtrueを返す関数
	 * @param human
	 * @return true or false
	 * @author 岡島
	 */
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
	
	//*********************************************************************************************************************************************************************************
	// calcSearch 移植
	
    private Action calcSearch(){
    	if (agentInfo.getTime() < scenarioInfo.getKernelAgentsIgnoreuntil()) {
            return null;
        }
        return getSearchAction(pathPlanning,this.agentInfo.getPosition(),this.unsearchedBuildingIDs);
    }

    private Action getSearchAction(PathPlanning pathPlanning, EntityID from,  Collection<EntityID> targets){
    	pathPlanning.setFrom(from);
    	pathPlanning.setDestination(targets);
    	List<EntityID> path = pathPlanning.calc().getResult();
    	previousPaths.add(path);

    	if(previousPaths.size()<2 || !isStopped(previousPaths.get(0),previousPaths.get(1))){
    		if (path != null && path.size() > 0){
    			StandardEntity entity = this.worldInfo.getEntity(path.get(path.size() - 1));
    			if (entity instanceof Building)
    			{
    				if (entity.getStandardURN() != StandardEntityURN.REFUGE)
    				{
    					path.remove(path.size() - 1);
    				}
    			}
    			movedTime.add(agentInfo.getTime());//動いた時のTimeを記録
    			
    			if(path.size() > 1)
    				return new ActionMove(path);
    		}
    		return null;
    	}
    	this.stopped = true;
    	reset();
    	return null;
    }

    // 止まってる判定はtrue、止まってなければfalse
    private boolean isStopped(List<EntityID> path1, List<EntityID> path2) {
    	Human agent = (Human)this.agentInfo.me();
    	previousLocations.add(new Point2D(agent.getX(),agent.getY()));//移動するときの場所を記録(0が現在地)
    	
    	if (path1 == null || path2 == null){
    		return false;
    	}
    	if (path1.size() != path2.size()) {
    		return false;
    	}else{
    		for (int i = 0; i < path1.size(); i++) {
    			EntityID id1 = path1.get(i);
    			EntityID id2 = path2.get(i);
    			if (!id1.equals(id2))
    				return false;
    		}
    	}

    	if(previousLocations.size()>2) {
    		return withinRange(previousLocations.get(0),previousLocations.get(1),previousLocations.get(2));
    	}
    	return false;
    }
    
    private boolean  withinRange(Point2D position1,Point2D position2,Point2D position3) {
        int range = 30000;
  
        double dist1 = GeometryTools2D.getDistance(position1, position2);
        double dist2 = GeometryTools2D.getDistance(position1, position3);

        if (dist1 < range && dist2 < range) {
            return true;
        }

        return false;
    }
    private void reset(){
    	this.unsearchedBuildingIDs.clear();
    	this.previousPaths.clear();
        this.previousLocations.clear();

    	if((this.agentInfo.getTime()!=0 && (this.agentInfo.getTime()%this.changeClusterCycle)==0)||stopped){
    		this.stopped=false;
    		this.clusterIndex = random.nextInt(clustering.getClusterNumber());
    		this.changeClusterCycle = random.nextInt(16) + 15;//変更

    	}
    	Collection<StandardEntity> clusterEntities = new ArrayList<>();
    	if(clustering!=null) {
    		clusterEntities.addAll(this.clustering.getClusterEntities(clusterIndex));
    	}

    	if(clusterEntities != null && clusterEntities.size() > 0) {
    		for(StandardEntity entity : clusterEntities) {
    			if(entity instanceof Building && entity.getStandardURN() != REFUGE) {
    				this.unsearchedBuildingIDs.add(entity.getID());
    			}
    		}
    	}else{
    		this.unsearchedBuildingIDs.addAll(this.worldInfo.getEntityIDsOfType(BUILDING));
    	}
    }
	
	//*********************************************************************************************************************************************************************************
	/**
	 * 指定2頂点がlength以内の距離であればtrueを返す関数
	 * @param ax
	 * @param ay
	 * @param bx
	 * @param by
	 * @param length
	 * @return true or false
	 */
	private boolean isNearly(int ax, int ay, int bx, int by, int length){
		if(Math.sqrt((ax-bx)*(ax-bx)+(ay-by)*(ay-by))<length){
			return true;
		}else{
			return false;
		}
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * その場で行動が停滞していないか調べる関数
	 * 停滞している場合trueを返す
	 *
	 * @return true or false
	 */
	private boolean isParalysis(){
		//基本的にアクションなどの保存は配列要素は3つを想定
		int count=0;
		for(int i=0;i<oldX.length-1;i++){
			//if(oldAction[i]!=null&&oldAction[i+1]!=null){
			//if(oldAction[i]==oldAction[i+1]){
			if(isNearly(oldX[i], oldY[i], oldX[i+1], oldY[i+1], 500)){
				//if(oldTargetX[i]==oldTargetX[i+1]&&oldTargetY[i]==oldTargetY[i+1]){
				count++;
				//}
			}
			//}
			//}
		}
		if(count==3){
			return true;
		}
		return false;
	}
	//*********************************************************************************************************************************************************************************
	private void registeredReset(){
		this.oldX[this.oldX.length-1]=-1;
		this.oldY[this.oldX.length-1]=-1;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * ・アクション・座標・目標座標を記録する
	 *
	 * @param ac
	 * @param x
	 * @param y
	 * @param targetX
	 * @param targetY
	 */
	private void registeredStamp(actionURN ac, int x, int y, int targetX, int targetY){
		for(int i=0;i<this.oldAction.length-1;i++){
			this.oldAction[i]=this.oldAction[i+1];
			this.oldX[i]=this.oldX[i+1];
			this.oldY[i]=this.oldY[i+1];
			this.oldTargetX[i]=this.oldTargetX[i+1];
			this.oldTargetY[i]=this.oldTargetY[i+1];
		}
		this.oldAction[this.oldAction.length-1]=ac;
		this.oldX[this.oldX.length-1]=x;
		this.oldY[this.oldY.length-1]=y;
		this.oldTargetX[this.oldTargetX.length-1]=targetX;
		this.oldTargetY[this.oldTargetY.length-1]=targetY;
	}
	//*********************************************************************************************************************************************************************************
	private Action calcAction(List<EntityID>path) {
		//事前にpathがnullもしくは空でないことを確認
		PoliceForce agent=(PoliceForce)this.agentInfo.me();
		path=modificationPath(path, agent.getPosition());
		//pathのサイズが1かそれ以外か(現在地＝目的地)かどうかを調べるということ
		int movableDistance=20000;
		int moveDistance=0;
		targetPoint=agentPoint;
		Point2D finalDestinationPoint=null;
		Point2D beforePoint=null;
		Point2D beforemodificatePoint=null;
		boolean useFinalDestinationPoint=false;
		if(path.size()>1){
			//pathが2個ほどになると動作が不安定になる
			Area areaA=null;
			Area areaB=null;
			for(int i=1;i<path.size();i++){
				areaA=toArea(path.get(i-1));
				areaB=toArea(path.get(i));
				beforePoint=targetPoint;
				targetPoint=getLowCostPoint(areaA, areaB, targetPoint);
				//exporter.exportLog_Point(targetPoint, 255, 100, 0, 255);
				targetPoint=modificationNearestPoint(areaA, targetPoint);
				if(i==path.size()-1&&this.targetEntityPoint!=null&&worldInfo.getEntity(this.target) instanceof Human) {
					targetPoint=this.targetEntityPoint;
				}
				//exporter.exportLog_Point(targetPoint, 255, 100, 0, 255);
				if(!isLineBlockade(areaA, areaB, beforePoint, targetPoint)){
					//直線上に瓦礫はないので移動可能
					moveDistance+=calcDistance(beforePoint, targetPoint);
					if(moveDistance>movableDistance){
						this.action=actionURN.actionMove;
						return calcMoveAction(path, areaA.getID(), targetPoint);
					}
				}else{
					//指定ポイント上もしくはその直線上に瓦礫がある
					boolean isLineBlockade=isLineBlockade(areaA, areaB, beforePoint, targetPoint);
					boolean isFrontBlockade=isFrontBlockade(areaA, areaB, agentPoint, targetPoint);
					//boolean isEfficientClear=isEfficientClear(areaA, areaB, beforePoint, targetPoint);
					if(moveDistance<5000&&isLineBlockade&&isFrontBlockade){
						this.action=actionURN.actionClear;
						return calcClearAction(targetPoint);
					}else{
						//exporter.exportLog_Line(beforePoint, targetPoint, 255, 255, 0, 255);
						targetPoint=modificationPoint(areaA, beforePoint, targetPoint);
						//exporter.exportLog_Point(targetPoint, 255, 255, 0, 255);
						this.action=actionURN.actionMove;
						return calcMoveAction(path, areaA.getID(), targetPoint);
					}
				}
			}
		}else {
			//パスがひとつしかないとき
			Area areaA=null;
			areaA=toArea(path.get(0));
			beforePoint=targetPoint;
			targetPoint=this.targetEntityPoint;
			if(targetPoint!=null) {
				//exporter.exportLog_Point(targetPoint, 255, 100, 0, 255);
				if(!isLineBlockade(areaA, areaA, beforePoint, targetPoint)){
					moveDistance+=calcDistance(beforePoint, targetPoint);
					if(moveDistance>movableDistance){
						this.action=actionURN.actionMove;
						return calcMoveAction(path, areaA.getID(), targetPoint);
					}
				}else{
					//指定ポイント上もしくはその直線上に瓦礫がある
					boolean isLineBlockade=isLineBlockade(areaA, areaA, beforePoint, targetPoint);
					boolean isFrontBlockade=isFrontBlockade(areaA, areaA, agentPoint, targetPoint);
					boolean isEfficientClear=isEfficientClear(areaA, areaA, beforePoint, targetPoint);
					if(moveDistance<5000&&isLineBlockade&&isFrontBlockade){
						this.action=actionURN.actionClear;
						return calcClearAction(targetPoint);
					}else{
						targetPoint=modificationPoint(areaA, beforePoint, targetPoint);
						//exporter.exportLog_Point(targetPoint, 255, 100, 0, 255);
						this.action=actionURN.actionMove;
						return calcMoveAction(path, areaA.getID(), targetPoint);
					}
				}
			}
		}
		this.action=actionURN.actionMove;
		return calcMoveAction(path, path.get(path.size()-1), targetPoint);
	}
	//*********************************************************************************************************************************************************************************
	/*private Action calcActionMove(List<EntityID>path) {
		Human agent=(Human)this.agentInfo.me();
		Point2D startPoint=new Point2D(agent.getX(), agent.getY());
		int movableDistance=20000;
		for(int i=0;i<path.size()-1;i++){
			Point2D targetPoint=getSelectableBoundaryPoint(toArea(path.get(i)), toArea(path.get(i+1)));
			if(targetPoint!=null){
				java.awt.geom.Area javaArea=new java.awt.geom.Area(toArea(path.get(0)).getShape());
				java.awt.geom.Area blockadeJavaArea=calcForbiddenArea(toArea(path.get(i)));
				java.awt.geom.Area blockadeArea=toBlockadeArea(toArea(path.get(i)));
				java.awt.geom.Area blockadeForbiddenArea=calcBlockadeForbiddenArea(toArea(path.get(i)));
				javaArea.subtract(blockadeArea);
				java.awt.geom.Area calcArea=new java.awt.geom.Area();
				calcArea.add(javaArea);
				java.awt.geom.Area straightArea=makeArea(startPoint, targetPoint, 1);
				straightArea.intersect(blockadeArea);
				if(straightArea.isEmpty()){
					//①瓦礫のエリアと直線エリアの交差エリアがない＝直線上に瓦礫はない
				}else{

				}
				makeArcArea(toArea(path.get(i)), startPoint, targetPoint, movableDistance, 60, 60);
				startPoint=targetPoint;
			}else{
				break;
			}
		}
		return null;
	}*/
	//*********************************************************************************************************************************************************************************
	/**
	 * 引数のEntityID(AreaのEntityID)上に他Humanがいないかどうかを調べる。
	 * いたら即その座標を返す
	 *
	 * @param entityID
	 * @return 目標Humanの座標
	 */
	private Point2D calcOnTarget(EntityID entityID){
		if(entityID!=null){
			Collection<StandardEntity> collection=worldInfo.getEntitiesOfType(StandardEntityURN.AMBULANCE_TEAM,StandardEntityURN.FIRE_BRIGADE,
					StandardEntityURN.POLICE_FORCE,StandardEntityURN.CIVILIAN);
			if(collection!=null){
				for(StandardEntity standardEntity:collection){
					if(standardEntity instanceof Human){
						Human human=(Human)standardEntity;
						if(human.getPosition()!=null){
							if(human.getPosition().equals(entityID)&&human.isXDefined()&&human.isYDefined()){
								return new Point2D(human.getX(), human.getY());
							}
						}
					}
				}
			}
		}
		return null;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * 点と線の距離を計算する
	 *
	 * @param edge
	 * @param point
	 * @return 距離(double)
	 */
	private double calcDistanceOfPointToLine(Edge edge, Point2D point){
		double x=point.getX();		double y=point.getY();
		double a=-(edge.getEndY()-edge.getStartY())/(edge.getEndX()-edge.getStartX());
		double b=1;		double c=-a*x-y;
		return Math.abs(a*x+b*y+c)/Math.hypot(a, y);
	}
	//*********************************************************************************************************************************************************************************
	/*public java.awt.geom.Area calcMovableFanArea(Point2D point, int radius,Area areaA, Area areaB){
		java.awt.geom.Area javaArea=calcMovableJavaAreaContainsPoint(toList(areaA.getID(), areaB.getID()), point);
		if(javaArea!=null){
			//javaArea.intersect(toJavaArea(point, radius));
			if(javaArea!=null){
				List<Edge> edges=toEdges(javaArea);
				edges=calcSubtractIntersectEdge(edges, point);
				if(isListContainsContentsEdge(edges)){
					int[] apexX=new int[edges.size()*2];
					int[] apexY=new int[edges.size()*2];
					for(int i=1;i<edges.size()*2;i+=2){
						apexX[i-1]=edges.get(i/2).getStartX();
						apexX[i]=edges.get(i/2).getEndX();
						apexY[i-1]=edges.get(i/2).getStartY();
						apexY[i]=edges.get(i/2).getEndY();
					}
					int distance=Integer.MAX_VALUE;
					Vector2D direction=toVector2D(agentPoint, getNearestPoint(areaA, areaB, point));
					Point2D targetPoint=null;
					for(int i=0;i<apexX.length;i++){
						int x=apexX[i];
						int y=apexY[i];
						int temp=(int)Math.hypot(areaB.getX()-x,areaB.getY()-y);				//現在基準点から遠いとしているがareaBの中心から近いに書き換えるほうが得策か？　修正済み
						if(temp<distance){	//※要修正必要　要検証の必要性大
							distance=temp;
						}
					}
					if(targetPoint!=null){
						return targetPoint;
					}
				}
			}
		}
		return null;
	}*/
	//*********************************************************************************************************************************************************************************
	/**
	 *
	 * 指定したポイントを含む指定エリアA,Bにおいてもっとも遠い指定距離以内の移動可能座標を求める
	 *
	 * @param point
	 * @param areaA
	 * @param areaB
	 * @return point
	 * @author 兼近
	 */
	private Point2D calcMovablePoint(Point2D pointA, Point2D pointB, java.awt.geom.Area area){
		if(area!=null){
			List<Edge> edges=toEdges(area);
			edges=calcSubtractIntersectEdge(edges, pointA);
			if(isListContainsContentsEdge(edges)){
				int[] apexX=new int[edges.size()*2];
				int[] apexY=new int[edges.size()*2];
				for(int i=1;i<edges.size()*2;i+=2){
					apexX[i-1]=edges.get(i/2).getStartX();
					apexX[i]=edges.get(i/2).getEndX();
					apexY[i-1]=edges.get(i/2).getStartY();
					apexY[i]=edges.get(i/2).getEndY();
				}
				int distance=Integer.MAX_VALUE;
				int angle=Integer.MAX_VALUE;
				Vector2D direction=toVector2D(pointA, pointB);
				Point2D targetPoint=null;
				for(int i=0;i<apexX.length;i++){
					int x=apexX[i];
					int y=apexY[i];
					int temp=(int)Math.hypot(pointB.getX()-x,pointB.getY()-y);				//現在基準点から遠いとしているがareaBの中心から近いに書き換えるほうが得策か？　修正済み
					Vector2D vector=toVector2D(pointA, toPoint2D(x,y));
					int tempAngle=(int)calcAngle(direction, vector);
					//if(temp-(angle-tempAngle)*20>distance&&tempAngle<angle){//マジックナンバーで無理やり
					//if(temp>distance&&tempAngle<90&&tempAngle<angle){
					if(temp<distance&&(tempAngle<angle||(angle-15<tempAngle&&tempAngle<angle+15))){	//※要修正必要　要検証の必要性大
						distance=temp;
						targetPoint=toPoint2D(x, y);
						angle=tempAngle;
					}
				}
				if(targetPoint!=null){
					return targetPoint;
				}
			}
		}
		return null;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 *
	 * 指定したポイントを含む指定エリアA,Bにおいてもっとも遠い指定距離以内の移動可能座標を求める
	 *
	 * @param point
	 * @param areaA
	 * @param areaB
	 * @return point
	 * @author 兼近
	 */
	private Point2D calcMovablePoint(Point2D point, int radius,Area areaA, Area areaB){
		java.awt.geom.Area javaArea=calcMovableJavaAreaContainsPoint(toList(areaA.getID(), areaB.getID()), point);
		if(javaArea!=null){
			//javaArea.intersect(toJavaArea(point, radius));
			if(javaArea!=null){
				List<Edge> edges=toEdges(javaArea);
				edges=calcSubtractIntersectEdge(edges, point);
				if(isListContainsContentsEdge(edges)){
					int[] apexX=new int[edges.size()*2];
					int[] apexY=new int[edges.size()*2];
					for(int i=1;i<edges.size()*2;i+=2){
						apexX[i-1]=edges.get(i/2).getStartX();
						apexX[i]=edges.get(i/2).getEndX();
						apexY[i-1]=edges.get(i/2).getStartY();
						apexY[i]=edges.get(i/2).getEndY();
					}
					int distance=Integer.MAX_VALUE;
					int angle=Integer.MAX_VALUE;
					Vector2D direction=toVector2D(agentPoint, getNearestPoint(areaA, areaB, point));
					Point2D targetPoint=null;
					for(int i=0;i<apexX.length;i++){
						int x=apexX[i];
						int y=apexY[i];
						int temp=(int)Math.hypot(areaB.getX()-x,areaB.getY()-y);				//現在基準点から遠いとしているがareaBの中心から近いに書き換えるほうが得策か？　修正済み
						Vector2D vector=toVector2D(point, toPoint2D(x,y));
						int tempAngle=(int)calcAngle(direction, vector);
						//if(temp-(angle-tempAngle)*20>distance&&tempAngle<angle){//マジックナンバーで無理やり
						//if(temp>distance&&tempAngle<90&&tempAngle<angle){
						//if(temp<distance&&(tempAngle<angle||(angle-15<tempAngle&&tempAngle<angle+15))){	//※要修正必要　要検証の必要性大
						if(temp<distance){
							distance=temp;
							targetPoint=toPoint2D(x, y);
							angle=tempAngle;
						}
					}
					if(targetPoint!=null){
						return targetPoint;
					}
				}
			}
		}
		return null;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 *
	 * 指定したポイントを含む指定エリアA,Bにおいて指定距離以内の移動可能エリアを求める
	 *
	 * @param point
	 * @param areaA
	 * @param areaB
	 * @return point
	 * @author 兼近
	 */
	private java.awt.geom.Area calcMovableArea(Point2D point, int radius,Area areaA, Area areaB){
		java.awt.geom.Area javaArea=calcMovableJavaAreaContainsPoint(toList(areaA.getID(), areaB.getID()), point);
		if(javaArea!=null){
			if(javaArea!=null){
				List<Edge> edges=toEdges(javaArea);
				edges=calcSubtractIntersectEdge(edges, point);
				return makeArea(edges);
			}
		}
		return null;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * 指定2エリアの境界上で指定座標から近い方の頂点を返す
	 *
	 * @param listA
	 * @param listB
	 * @return list
	 * @author 兼近
	 */
	private Point2D getNearestPoint(Area areaA, Area areaB, Point2D point){
		Edge edge=areaA.getEdgeTo(areaB.getID());
		if(edge!=null){
			double distanceA=calcDistance(edge.getStart(), point);
			double distanceB=calcDistance(edge.getEnd(), point);
			if(distanceA<distanceB){
				return edge.getStart();
			}else{
				return edge.getEnd();
			}
		}
		return null;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * 指定2エリアの境界上で指定座標から近い方の頂点を返す
	 * (ただしおおよそ計算のためそこそこの誤差が出るため、他精密修正関数との併用を推奨)
	 *
	 * @param areaA
	 * @param areaB
	 * @param point
	 * @return
	 */
	private Point2D getNearestPointAvoidForbbidenArea(Area areaA, Area areaB, Point2D point){
		Edge edge=areaA.getEdgeTo(areaB.getID());
		if(edge!=null){
			double distanceA=calcDistance(edge.getStart(), point);
			double distanceB=calcDistance(edge.getEnd(), point);
			if(distanceA<distanceB){
				return toPoint2D(edge.getStart(), toVector2D(edge.getStart(), edge.getEnd()).normalised().scale(500));
			}else{
				return toPoint2D(edge.getEnd(), toVector2D(edge.getEnd(), edge.getStart()).normalised().scale(500));
			}
		}
		return null;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * 指定2エリアの境界上の２点のうち指定頂点と結んだ直線上の瓦礫の少ない方を選ぶ
	 *
	 * @param areaA
	 * @param areaB
	 * @param point
	 * @return
	 */
	private Point2D getLowCostPoint(Area areaA, Area areaB, Point2D point){
		Edge edge=areaA.getEdgeTo(areaB.getID());
		if(edge!=null){
			java.awt.geom.Area blockadeArea=toBlockadeArea(areaA);
			if(blockadeArea!=null&&!blockadeArea.isEmpty()){
				//瓦礫が存在するとき
				java.awt.geom.Area javaAreaA=new java.awt.geom.Area(makeArea(point, edge.getStart(), 2));
				java.awt.geom.Area javaAreaB=new java.awt.geom.Area(makeArea(point, edge.getEnd(), 2));
				javaAreaA.intersect(blockadeArea);
				javaAreaB.intersect(blockadeArea);
				double surfaceA=surface(javaAreaA);
				double surfaceB=surface(javaAreaB);
				if(surfaceA<surfaceB){
					return toPoint2D(edge.getStart(), toVector2D(edge.getStart(), edge.getEnd()).normalised().scale(500));
				}else{
					return toPoint2D(edge.getEnd(), toVector2D(edge.getEnd(), edge.getStart()).normalised().scale(500));
				}
			}
			//瓦礫がないor瓦礫の量が同じor瓦礫があってもどちらの点への直線上にもない、ときは以下の処理を行う
			double distanceA=calcDistance(edge.getStart(), point);
			double distanceB=calcDistance(edge.getEnd(), point);
			if(distanceA<distanceB){
				return toPoint2D(edge.getStart(), toVector2D(edge.getStart(), edge.getEnd()).normalised().scale(500));
			}else{
				return toPoint2D(edge.getEnd(), toVector2D(edge.getEnd(), edge.getStart()).normalised().scale(500));
			}
		}
		return null;
	}
	//*********************************************************************************************************************************************************************************
	private Point2D getNearestPointAlongEdge(Area areaA, Area areaB, Point2D point){
		Point2D resultPoint=null;
		if(areaA!=null&&areaB!=null&&point!=null){
			Edge boundary=areaA.getEdgeTo(areaB.getID());
			if(boundary!=null){
				List<Edge> edges=areaA.getEdges();
				double distance=Integer.MAX_VALUE;
				Edge alongEdge=null;
				for(Edge edge:edges){
					double temp=calcDistanceOfPointToLine(edge, point);
					if(temp<distance&&!edge.equals(boundary)){
						distance=temp;
						alongEdge=edge;
					}
				}
				if(alongEdge!=null){
					Point2D a=boundary.getStart();
					Point2D b=boundary.getEnd();
					Point2D c=alongEdge.getStart();
					Point2D d=alongEdge.getEnd();
					//製作途中
				}
			}
		}
		return resultPoint;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * 指定２頂点の間の瓦礫の面積を求める
	 * @param areaA
	 * @param areaB
	 * @param pointA
	 * @param pointB
	 * @return 瓦礫面積
	 */
	private double calcFrontBlockadeSurface(Area areaA, Area areaB,Point2D pointA,Point2D pointB){
		if(calcDistance(pointA, pointB)!=scenarioInfo.getClearRepairDistance()){
			pointB=toPoint2D(pointA, toVector2D(pointA, pointB).normalised().scale(scenarioInfo.getClearRepairDistance()));
		}
		java.awt.geom.Area javaArea=new java.awt.geom.Area();
		javaArea.add(toBlockadeArea(areaA));
		javaArea.add(toBlockadeArea(areaB));
		javaArea.intersect(makeArea(pointA, pointB, scenarioInfo.getClearRepairRad()*2));
		return surface(javaArea);
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * 指定２頂点間で長さが瓦礫撤去可能距離/2で幅が極小なエリア上に瓦礫があるか
	 * (つまりは瓦礫が少ないor薄くても正面を塞いでいるかどうか)
	 * (trueで瓦礫あり、falseで瓦礫なし)
	 *
	 * @param areaA
	 * @param areaB
	 * @param pointA
	 * @param pointB
	 * @return true or false
	 */
	private boolean isFrontBlockade(Area areaA, Area areaB,Point2D pointA,Point2D pointB){
		if(calcDistance(pointA, pointB)>scenarioInfo.getClearRepairDistance()){
			pointB=toPoint2D(pointA, toVector2D(pointA, pointB).normalised().scale(scenarioInfo.getClearRepairDistance()/2));
		}
		java.awt.geom.Area javaArea=new java.awt.geom.Area();
		javaArea.add(toBlockadeArea(areaA));
		javaArea.add(toBlockadeArea(areaB));
		javaArea.intersect(makeArea(pointA, pointB, 3));
		double surface=surface(javaArea);
		if(surface>0) {
			return true;
		}
		return false;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * 指定２頂点間を結ぶ幅が極小なエリア上に瓦礫があるか
	 * (つまりは瓦礫が少ないor薄くても正面を塞いでいるかどうか)
	 * (trueで瓦礫あり、falseで瓦礫なし)
	 *
	 * @param areaA
	 * @param areaB
	 * @param pointA
	 * @param pointB
	 * @return true or false
	 */
	private boolean isLineBlockade(Area areaA, Area areaB,Point2D pointA,Point2D pointB){
		java.awt.geom.Area javaArea=new java.awt.geom.Area();
		javaArea.add(toBlockadeArea(areaA));
		javaArea.add(toBlockadeArea(areaB));
		javaArea.intersect(makeArea(pointA, pointB, 3));
		double surface=surface(javaArea);
		if(surface>0) {
			return true;
		}
		return false;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * 指定2頂点を結ぶエリア上において単純に2頂点を結んでできた長さ制限なしのエリアと、
	 * 瓦礫の撤去可能距離までの長さのエリア上の瓦礫の量が同じならばTrueを返す
	 * (つまりは瓦礫が遠かった場合瓦礫撤去範囲にあっても移動を優先するためそれ以上遠くに瓦礫がないのなら
	 * 先に瓦礫を消すほうが一回の移動量が増えるため効率が良い)
	 *
	 * @param areaA
	 * @param areaB
	 * @param pointA
	 * @param pointB
	 * @return true or false
	 */
	private boolean isEfficientClear(Area areaA, Area areaB,Point2D pointA,Point2D pointB){
		Point2D limitedPointA=clonePoint2D(pointA);
		Point2D limitedPointB=clonePoint2D(pointB);
		if(calcDistance(pointA, pointB)>scenarioInfo.getClearRepairDistance()){
			limitedPointB=toPoint2D(pointA, toVector2D(pointA, pointB).normalised().scale(scenarioInfo.getClearRepairDistance()/2));
		}
		java.awt.geom.Area javaArea=new java.awt.geom.Area();
		javaArea.add(toBlockadeArea(areaA));
		javaArea.add(toBlockadeArea(areaB));
		java.awt.geom.Area makeAreaA=makeArea(pointA, pointB, 3);
		java.awt.geom.Area makeAreaB=makeArea(limitedPointA, limitedPointB, 3);
		makeAreaA.intersect(javaArea);
		makeAreaB.intersect(javaArea);
		double lineSurface=surface(makeAreaA);
		double clearSurface=surface(makeAreaB);
		if(lineSurface==clearSurface) {
			return true;
		}
		return false;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * Point2Dをクローン(アドレス参照回避)
	 *
	 * @param point
	 * @return point2D
	 * @author 兼近
	 */
	private Point2D clonePoint2D(Point2D point){
		if(point!=null){
			return new Point2D(point.getX(), point.getY());
		}
		return null;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * ADFのAreaをクローン(アドレス参照回避)
	 *
	 * @param area
	 * @return Area
	 * @author 兼近
	 */
	private Area cloneArea(Area area){
		return area;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * 指定したリストにもう一方のリストの値を追加する
	 *
	 * @param listA
	 * @param listB
	 * @return list
	 * @author 兼近
	 */
	private List<EntityID> addList(List<EntityID>listA, List<EntityID>listB){
		if(listA!=null&&!listA.isEmpty()&&listB!=null&&!listB.isEmpty()){
			for(EntityID entityID:listB){
				listA.add(entityID);
			}
			return listA;
		}
		return listA;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * 指定エリア上の指定頂点から最も近い禁止エリアを弾いた頂点を選ぶ
	 *
	 * @param area
	 * @param pointA
	 * @return
	 */
	private Point2D modificationNearestPoint(Area area, Point2D pointA){
		java.awt.geom.Area javaArea=new java.awt.geom.Area();
		javaArea.add(toArea(area));
		javaArea.subtract(calcForbiddenArea(area));
		Point2D nearestPoint=null;
		if(javaArea!=null&&!javaArea.isEmpty()) {
			PathIterator pathIterator = javaArea.getPathIterator(null);
			List<Point2D> pointList = new ArrayList<>();
			while (!pathIterator.isDone()) {
				while (!pathIterator.isDone()) {
					double points[] = new double[2];
					int type = pathIterator.currentSegment(points);
					pathIterator.next();
					if (type == PathIterator.SEG_CLOSE) {
						break;
					}
					Point2D apex=new Point2D(points[0], points[1]);
					pointList.add(apex);
				}
			}
			if(pointList!=null&&!pointList.isEmpty()) {
				int nearestDistance=Integer.MAX_VALUE;
				for(Point2D apex:pointList) {
					int distance=(int)Math.hypot(pointA.getX()-apex.getX(), pointA.getY()-apex.getY());
					if(distance<nearestDistance) {
						nearestDistance=distance;
						nearestPoint=apex;
					}
				}
			}
		}
		if(nearestPoint==null){
			nearestPoint=pointA;
		}
		return nearestPoint;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * 指定エリア上の指定2頂点を結んだ線状の禁止エリア、瓦礫、瓦礫禁止エリアを除いた直線エリア内のもっとも指定2頂点目に近い点を選ぶ
	 *
	 * @param area
	 * @param pointA
	 * @param pointB
	 * @return 頂点
	 * @author 兼近
	 */
	private Point2D modificationPoint(Area area, Point2D pointA, Point2D pointB){
		java.awt.geom.Area javaArea=new java.awt.geom.Area();
		javaArea.add(toArea(area));
		javaArea.subtract(toBlockadeArea(area));
		javaArea.intersect(makeArea(pointA, pointB, 100));
		javaArea=calcAreaContainsPoint(javaArea, pointA);
		if(javaArea!=null&&!javaArea.isEmpty()) {
			javaArea.subtract(calcForbiddenArea(area));
			javaArea.subtract(calcBlockadeForbiddenArea(area));
			if(javaArea!=null&&!javaArea.isEmpty()) {
				return calcNearestPoint(javaArea, pointB);

			}
		}
		return pointB;
	}
	//*********************************************************************************************************************************************************************************
	private Point2D calcNearestPoint(java.awt.geom.Area area, Point2D point) {
		Point2D nearestPoint=null;
		if(area!=null&&!area.isEmpty()) {
			PathIterator pathIterator = area.getPathIterator(null);
			List<Point2D> pointList = new ArrayList<>();
			while (!pathIterator.isDone()) {
				while (!pathIterator.isDone()) {
					double points[] = new double[2];
					int type = pathIterator.currentSegment(points);
					pathIterator.next();
					if (type == PathIterator.SEG_CLOSE) {
						break;
					}
					pointList.add(toPoint2D(points[0], points[1]));
				}
			}
			if(pointList!=null&&!pointList.isEmpty()) {
				double distance=Integer.MAX_VALUE;
				for(Point2D temp:pointList) {
					double tempDistance=calcDistance(point, temp);
					if(tempDistance<distance) {
						distance=tempDistance;
						nearestPoint=clonePoint2D(temp);
					}
				}
			}
		}
		return nearestPoint;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * 指定エリアから指定の頂点を含む閉鎖エリアを返す
	 * (ない場合はnull)
	 *
	 * @param area
	 * @param point
	 * @return 指定頂点を含むエリア
	 */
	private java.awt.geom.Area calcAreaContainsPoint(java.awt.geom.Area area, Point2D point) {
		if(area!=null&&point!=null) {
			PathIterator pathIterator=area.getPathIterator(null);
			while(!pathIterator.isDone()) {
				List<Point2D>pointList=new ArrayList<>();
				while(!pathIterator.isDone()) {
					double[] points=new double[2];
					int type=pathIterator.currentSegment(points);
					pathIterator.next();
					if(type==PathIterator.SEG_CLOSE) {
						break;
					}
					Point2D tempPoint=new Point2D(points[0], points[1]);
					pointList.add(tempPoint);
				}
				if(pointList!=null&&!pointList.isEmpty()) {
					int[] apexX=new int[pointList.size()];
					int[] apexY=new int[pointList.size()];
					for(int i=0;i<pointList.size();i++) {
						Point2D apex=pointList.get(i);
						apexX[i]=(int)apex.getX();
						apexY[i]=(int)apex.getY();
					}
					java.awt.geom.Area tempArea_Java=new java.awt.geom.Area(new java.awt.Polygon(apexX, apexY, apexX.length));
					if(tempArea_Java.contains(point.getX(), point.getY())||tempArea_Java.intersects(point.getX()-1,point.getY()-1,3,3)) {
						return tempArea_Java;
					}
				}
			}
		}
		return null;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * 指定したエリア内の指定したポイントにもっとも近い到達可能なポイントを返す
	 *
	 * @param pointArea
	 * @param point
	 * @param movableRadius
	 * @return 到達可能座標
	 */
	private Point2D modificationPoint(Area pointArea, Point2D point,int movableRadius) {
		PoliceForce agent=(PoliceForce)this.agentInfo.me();
		int split=60;
		int[] apexX=new int[split];
		int[] apexY=new int[split];
		int radiansValue=360/split;
		for(int i=0;i<split;i++) {
			apexX[i]=agent.getX()+(int)(movableRadius*Math.cos(Math.toRadians(radiansValue*i)));
			apexY[i]=agent.getY()+(int)(movableRadius*Math.sin(Math.toRadians(radiansValue*i)));
		}
		java.awt.geom.Area safetyArea_Java=new java.awt.geom.Area(pointArea.getShape());
		safetyArea_Java.subtract(getForbiddenAreas(pointArea));
		java.awt.geom.Area blockadeArea_Java=calcBlockadeForbiddenArea(pointArea);
		if(blockadeArea_Java!=null) {
			//exporter.exportLog_Polygon("forbidden", blockadeArea_Java, 255, 0, 0, 50);
			safetyArea_Java.subtract(blockadeArea_Java);
		}
		java.awt.geom.Area movableArea=new java.awt.geom.Area(new java.awt.Polygon(apexX, apexY, apexX.length));
		movableArea.intersect(safetyArea_Java);
		if(movableArea!=null) {
			//exporter.exportLog_Polygon("Movable", movableArea, 0, 255, 0, 100);
			PathIterator pathIterator = movableArea.getPathIterator(null);
			List<Point2D> pointList = new ArrayList<>();
			while (!pathIterator.isDone()) {
				while (!pathIterator.isDone()) {
					double points[] = new double[2];
					int type = pathIterator.currentSegment(points);
					pathIterator.next();
					if (type == PathIterator.SEG_CLOSE) {
						break;
					}
					Point2D apex=new Point2D(points[0], points[1]);
					pointList.add(apex);
				}
			}
			if(pointList!=null&&!pointList.isEmpty()) {
				Point2D nearestPoint=null;
				int nearestDistance=Integer.MAX_VALUE;
				for(Point2D apex:pointList) {
					int distance=(int)Math.hypot(point.getX()-apex.getX(), point.getY()-apex.getY());
					if(distance<nearestDistance) {
						nearestDistance=distance;
						nearestPoint=apex;
					}
				}
				if(nearestPoint!=null) {
					////System.out.println("最寄りの安全エリアの点が見つけられた。判定は"+safetyAreas.get(pointArea.getID()).contains((int)nearestPoint.getX(),(int)nearestPoint.getY()));
					return nearestPoint;
				}
			}
		}
		return point;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * 指定したエリア群から指定したポイントを含む閉鎖エリアを返す
	 *
	 * @param areas
	 * @param point
	 * @return 閉鎖エリア
	 * @author 兼近
	 */
	private java.awt.geom.Area calcMovableJavaAreaContainsPoint(List<EntityID> areas, Point2D point) {
		if(isListContainsContentsEntityID(areas)){
			java.awt.geom.Area javaArea=new java.awt.geom.Area();
			for(int i=0;i<areas.size();i++){
				Area area=toArea(areas.get(i));
				javaArea.add(calcAreaSubtractBlockadeArea(area));
			}
			if(javaArea!=null) {
				PathIterator pathIterator = javaArea.getPathIterator(null);
				while (!pathIterator.isDone()) {
					List<double[]> pointList = new ArrayList<double[]>();
					while (!pathIterator.isDone()) {
						double points[] = new double[2];
						int type = pathIterator.currentSegment(points);
						pathIterator.next();
						if (type == PathIterator.SEG_CLOSE) {
							break;
						}
						pointList.add(points);
					}
					java.awt.geom.Area tempArea=toJavaArea(pointList);
					if(tempArea.contains(point.getX(),point.getY())||tempArea.intersects(point.getX()-1, point.getY()-1, 3, 3)){
						return tempArea;
					}
				}
			}
		}
		return null;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * 瓦礫の正面側(ポイントから見た表面が交差しているかどうかを判定する)
	 *
	 * @param area
	 * @param basePoint
	 * @return
	 */
	private List<Edge> calcBlockadeFrontEdge(Area area, Point2D basePoint){
		List<EntityID> blockadeIDs=area.getBlockades();
		if(isListContainsContentsEntityID(blockadeIDs)){
			List<Edge> edges=new ArrayList<>();
			for(EntityID blockadeID:blockadeIDs){
				Blockade blockade=toBlockade(blockadeID);
				edges=toEdges(edges,blockade.getApexes());
			}
			if(isListContainsContentsEdge(edges)){
				List<Edge>frontEdge=new ArrayList<>();
				for(Edge edge:edges){
					Edge edgeA=new Edge((int)basePoint.getX(), (int)basePoint.getY(), ((edge.getEndX()+edge.getStartX())/2),((edge.getEndY()+edge.getStartY())/2));
					boolean intersect=false;
					for(Edge edgeB:edges){
						if(isIntersection(edgeA, edgeB)){
							intersect=true;
							break;
						}
					}
					if(!intersect){
						frontEdge.add(edge);
					}
				}
				return frontEdge;
			}
		}
		return null;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * 指定Edge群のうちEdgeの中点と指定座標を結んだ線が他のEdgeと交わらないものだけを抽出する
	 *
	 * @param edges
	 * @param point
	 * @return 非交差Edge
	 */
	private List<Edge> calcSubtractIntersectEdge(List<Edge> edges, Point2D point){
		if(isListContainsContentsEdge(edges)){
			List<Edge>noIntersectEdge=new ArrayList<>();
			for(Edge edge:edges){
				Edge edgeA=new Edge((int)point.getX(), (int)point.getY(), edge.getStartX(), edge.getStartY());
				Edge edgeB=new Edge((int)point.getX(), (int)point.getY(), edge.getEndX(), edge.getEndY());
				int intersectA=0;
				int intersectB=0;
				for(Edge edgeD:edges){
					if(intersectA==0&&isIntersection(edgeA, edgeD)){
						intersectA=1;
					}
					if(intersectB==0&&isIntersection(edgeB, edgeD)){
						intersectB=1;
					}
				}
				if(intersectA+intersectB<=1){
					if(intersectA+intersectB==1){
						Vector2D vector;
						Point2D sPoint;
						if(intersectA==1){
							vector=new Vector2D(edge.getStartX()-edge.getEndX(), edge.getStartY()-edge.getEndY());
							sPoint=edge.getEnd();
						}else{
							vector=new Vector2D(edge.getEndX()-edge.getStartX(), edge.getEndY()-edge.getStartY());
							sPoint=edge.getStart();
						}
						int length=(int)vector.getLength();
						vector=vector.normalised();
						for(int i=1;i<10;i++){
							Vector2D temp=vector.scale((length/10)*(10-i));
							Edge tempEdge=new Edge((int)point.getX(), (int)point.getY(), (int)(sPoint.getX()+temp.getX()), (int)(sPoint.getY()+temp.getY()));
							boolean intersect=false;
							for(Edge edgeD:edges){
								if(isIntersection(tempEdge, edgeD)){
									intersect=true;
									break;
								}
							}
							if(!intersect){
								if(intersectA==1){
									edge=new Edge(tempEdge.getEnd(),sPoint);
								}else{
									edge=new Edge(sPoint, tempEdge.getEnd());
								}
								break;
							}
						}
					}
					noIntersectEdge.add(edge);
					//exporter.exportLog_Line("zzz", edge.getStartX(), edge.getStartY(), edge.getEndX(), edge.getEndY(), 255, 255, 0, 255);
				}
			}
			return noIntersectEdge;
		}
		return null;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * ２つのEdgeが交差するかどうかを判定する
	 * trueで交差 falseで非交差
	 *
	 * @param edgeA
	 * @param edgeB
	 * @return true or false
	 * @author 兼近
	 */
	private boolean isIntersection(Edge edgeA ,Edge edgeB){
		if(edgeA!=null&&edgeB!=null){
			int ax=edgeA.getStartX(); int bx=edgeA.getEndX(); int ay=edgeA.getStartY(); int by=edgeA.getEndY();
			int cx=edgeB.getStartX(); int dx=edgeB.getEndX(); int cy=edgeB.getStartY(); int dy=edgeB.getEndY();
			double ta = (cx - dx) * (ay - cy) + (cy - dy) * (cx - ax);
			double tb = (cx - dx) * (by - cy) + (cy - dy) * (cx - bx);
			double tc = (ax - bx) * (cy - ay) + (ay - by) * (ax - cx);
			double td = (ax - bx) * (dy - ay) + (ay - by) * (ax - dx);
			return tc * td < 0 && ta * tb < 0;
		}
		return false;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 *
	 * 2つのエリアが交差しているかどうか判定する
	 * trueで交差 falseで非交差
	 * @param areaA
	 * @param areaB
	 * @return
	 * @author 兼近
	 */
	private boolean isIntersection(java.awt.geom.Area areaA ,java.awt.geom.Area areaB){
		if(areaA!=null&&areaB!=null){
			areaA.intersect(areaB);
			if(areaA!=null&&!areaA.isEmpty()){
				return true;
			}
		}
		return false;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * 指定のパスを指定のIDまでにカットする(指定IDは含む)
	 *
	 * @param entityID
	 * @return 単体EntityIDのList
	 * @author 兼近
	 */
	private List<EntityID> listCut(List<EntityID> list, EntityID entityID){
		List<EntityID> result=new ArrayList<>();
		for(int i=0;i<list.size();i++){
			result.add(list.get(i));
			if(entityID.equals(list.get(i))){
				return result;
			}
		}
		return null;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * 単体のEntityIDをListに変換する
	 *
	 * @param entityID
	 * @return 単体EntityIDのList
	 * @author 兼近
	 */
	private List<EntityID> toList(EntityID entityID){
		List<EntityID> list=new ArrayList<>();
		list.add(entityID);
		return list;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * 2つのEntityIDをListに変換する
	 *
	 * @param entityA
	 * @param entityB
	 * @return 2値の入ったList
	 * @author 兼近
	 */
	private List<EntityID> toList(EntityID entityA,EntityID entityB){
		List<EntityID> list=new ArrayList<>();
		list.add(entityA);
		list.add(entityB);
		return list;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * 3つのEntityIDをListに変換する
	 *
	 * @param entityA
	 * @param entityB
	 * @param entityC
	 * @return 3値の入ったList
	 * @author 兼近
	 */
	private List<EntityID> toList(EntityID entityA,EntityID entityB,EntityID entityC){
		List<EntityID> list=new ArrayList<>();
		list.add(entityA);
		list.add(entityB);
		list.add(entityC);
		return list;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * EdgeのListをLine2DのListに変換する
	 *
	 * @param apexes
	 * @return Line2DのList
	 * @author 兼近
	 */
	private List<Line2D> toLine2Ds(List<Edge> list){
		if(list!=null&&!list.isEmpty()){
			List<Line2D>line2ds=new ArrayList<>();
			for(Edge edge:list){
				Line2D line2d=edge.getLine();
				line2ds.add(line2d);
			}
			return line2ds;
		}
		return null;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * 頂点群から順次つなげてEdgeに変換し、代入して返す
	 *
	 * @param apexes
	 * @return edgeのList
	 */
	private List<Edge> toEdges(List<Edge> list, int[] apexes){
		if(apexes!=null&&apexes.length>=4){
			if(!isListContainsContentsEdge(list)){
				list=new ArrayList<>();
			}
			for(int i=3;i<apexes.length;i+=2){
				Edge edge=new Edge(apexes[i-3], apexes[i-2], apexes[i-1], apexes[i]);
				list.add(edge);
			}
			Edge edge=new Edge(apexes[0], apexes[1], apexes[apexes.length-2], apexes[apexes.length-1]);
			list.add(edge);
			return list;
		}
		return null;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * javaのAreaの辺をEdgeに変換する
	 *
	 * @param apexes
	 * @return edgeのList
	 * @author 兼近
	 */
	private List<Edge> toEdges(java.awt.geom.Area area){
		if(area!=null){
			PathIterator pathIterator=area.getPathIterator(null);
			List<Edge> edges=new ArrayList<>();
			while(!pathIterator.isDone()){
				List<double[]> pointList=new ArrayList<double[]>();
				while(!pathIterator.isDone()){
					double[] points=new double[2];
					int type=pathIterator.currentSegment(points);
					pathIterator.next();
					if(type==PathIterator.SEG_CLOSE){
						break;
					}
					pointList.add(points);
				}
				if(pointList!=null&&!pointList.isEmpty()){
					for(int i=1;i<pointList.size();i++){
						edges.add(new Edge((int)pointList.get(i-1)[0], (int)pointList.get(i-1)[1], (int)pointList.get(i)[0], (int)pointList.get(i)[1]));
					}
					edges.add(new Edge((int)pointList.get(0)[0], (int)pointList.get(0)[1], (int)pointList.get(pointList.size()-1)[0], (int)pointList.get(pointList.size()-1)[1]));
				}
			}
			return edges;
		}
		return null;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * Listがnullでなく、空でもないときだけtrueを返す
	 *
	 * @param list
	 * @return true or false
	 */
	private boolean isListContainsContentsEntityID(List<EntityID> list){
		if(list!=null&&!list.isEmpty()){
			return true;
		}
		return false;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * Listがnullでなく、空でもないときだけtrueを返す
	 *
	 * @param list
	 * @return true or false
	 */
	private boolean isListContainsContentsEdge(List<Edge> list){
		if(list!=null&&!list.isEmpty()){
			return true;
		}
		return false;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * Listがnullでなく、空でもないときだけtrueを返す
	 *
	 * @param list
	 * @return true or false
	 */
	private boolean isListContainsContentsLine2D(List<Line2D> list){
		if(list!=null&&!list.isEmpty()){
			return true;
		}
		return false;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * Listがnullでなく、空でもないときだけtrueを返す
	 *
	 * @param list
	 * @return true or false
	 */
	private boolean isListContainsContentsPoint2D(List<Point2D> list){
		if(list!=null&&!list.isEmpty()){
			return true;
		}
		return false;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * 指定したエリアの指定座標を目指すActionMoveを返す
	 *
	 * @param path
	 * @return ActionMove(path,x,y);
	 */
	private Action calcActionMove(List<EntityID> path, Point2D point) {
		return new ActionMove(path, (int)point.getX(), (int)point.getY());
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * 指定2頂点の距離を求める
	 *
	 * @param pointA
	 * @param pointB
	 * @return 距離(Double型)
	 */
	private double calcDistance(Point2D pointA, Point2D pointB){
		if(pointA!=null&&pointB!=null){
			return Math.hypot(pointB.getX()-pointA.getX(), pointB.getY()-pointA.getY());
		}
		return -1;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * 指定した判定エリア1内の基準点から指定エリア2に行くことが可能か調べる(通行可能判定)
	 *
	 * @param firstPoint 基準点
	 * @param areaA 判定エリア1
	 * @param areaB 判定エリア2
	 * @return true or false
	 * @author 兼近
	 */
	private boolean calcMovablePathArea(Point2D pointA,Area areaA, Area areaB) {
		java.awt.geom.Area javaArea=new java.awt.geom.Area();
		javaArea.add(calcAreaSubtractBlockadeArea(areaA));
		javaArea.add(calcAreaSubtractBlockadeArea(areaB));
		Point2D pointB=getSelectableBoundaryPoint(areaA, areaB);
		isPoint=pointB;
		if(pointA!=null&&pointB!=null) {
			PathIterator pathIterator=javaArea.getPathIterator(null);
			List<Point2D> pointList=new ArrayList<>();
			while(!pathIterator.isDone()) {
				double[] tempPoints=new double[2];
				int type=pathIterator.currentSegment(tempPoints);
				if(type==PathIterator.SEG_CLOSE) {
					break;
				}
				pointList.add(new Point2D(tempPoints[0], tempPoints[1]));
				pathIterator.next();
			}
			int[] apexX=new int[pointList.size()];
			int[] apexY=new int[pointList.size()];
			for(int i=0;i<pointList.size();i++){
				apexX[i]=(int)pointList.get(i).getX();
				apexY[i]=(int)pointList.get(i).getY();
			}
			java.awt.geom.Area tempArea=new java.awt.geom.Area(new Polygon(apexX, apexY, apexX.length));
			if(tempArea!=null&&!tempArea.isEmpty()){
				if(calcContainsPoint(tempArea, pointA, pointB)) {
					return true;
				}
			}
		}
		return false;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * 引数3つのADFのAreaから2つ目のエリアから3つ目のエリアに移動することができるかを判定する
	 *
	 * @param beforeArea 直前のエリア(進行方向を決定する)
	 * @param firstArea 判定用エリア1
	 * @param secondArea 判定用エリア2
	 * @return boolean値
	 * @author 兼近
	 */
	private boolean calcMovablePathArea(Area beforeArea,Area firstArea, Area secondArea) {
		Human human=(Human)this.agentInfo.me();
		java.awt.geom.Area javaArea=new java.awt.geom.Area();
		Point2D firstPoint=getSelectableBoundaryPoint(firstArea, beforeArea);
		javaArea.add(this.isArea);
		if(beforeArea.getID().equals(firstArea.getID())){
			firstPoint=new Point2D(human.getX(), human.getY());
		}
		Point2D secondPoint=getSelectableBoundaryPoint(firstArea, secondArea);
		javaArea.add(this.isArea);
		if(firstPoint!=null&&secondPoint!=null) {
			PathIterator pathIterator=javaArea.getPathIterator(null);
			List<Point2D> pointList=new ArrayList<>();
			while(!pathIterator.isDone()) {
				double[] tempPoints=new double[2];
				int type=pathIterator.currentSegment(tempPoints);
				if(type==PathIterator.SEG_CLOSE) {
					break;
				}
				pointList.add(new Point2D(tempPoints[0], tempPoints[1]));
				pathIterator.next();
			}
			int[] apexX=new int[pointList.size()];
			int[] apexY=new int[pointList.size()];
			for(int i=0;i<pointList.size();i++){
				apexX[i]=(int)pointList.get(i).getX();
				apexY[i]=(int)pointList.get(i).getY();
			}
			java.awt.geom.Area tempArea=new java.awt.geom.Area(new Polygon(apexX, apexY, apexX.length));
			if(tempArea!=null&&!tempArea.isEmpty()){
				if(calcContainsPoint(tempArea, firstPoint, secondPoint)) {
					return true;
				}
			}
		}
		return false;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * 引数のADFのArea2つの境界のEdge付近のうち瓦礫のないエリアのポイントを選ぶ。
	 * (ただし返るポイントはエリアB側のポイントとする)
	 * なければnullを返す
	 *
	 * @param areaA
	 * @param areaB
	 * @return Edge付近の非瓦礫範囲の頂点
	 * @author 兼近
	 */
	private Point2D getSelectableBoundaryPoint(Area areaA, Area areaB) {
		Edge edge=areaA.getEdgeTo(areaB.getID());
		Point2D resultPoint=null;
		double distance=Integer.MAX_VALUE;
		if(edge!=null) {
			Point2D pointA=edge.getStart();
			Point2D pointB=edge.getEnd();
			java.awt.geom.Area edgeArea=makeArea(pointA, pointB, 3);
			java.awt.geom.Area javaArea=new java.awt.geom.Area(areaA.getShape());
			javaArea.add(new java.awt.geom.Area(areaB.getShape()));
			edgeArea.intersect(javaArea);
			java.awt.geom.Area blockadeArea=toBlockadeArea(areaA);
			blockadeArea.add(toBlockadeArea(areaB));
			edgeArea.subtract(blockadeArea);
			double surface=surface(edgeArea);
			if(surface>0){
				PathIterator pathIterator=edgeArea.getPathIterator(null);
				while(!pathIterator.isDone()) {
					double[] tempPoints=new double[2];
					int type=pathIterator.currentSegment(tempPoints);
					if(type!=PathIterator.SEG_CLOSE) {
						Point2D tempPoint=new Point2D(tempPoints[0], tempPoints[1]);
						double tempDistance=calcDistance(agentPoint, tempPoint);
						if(tempDistance<distance) {
							distance=tempDistance;
							resultPoint=clonePoint2D(tempPoint);
						}
					}
					pathIterator.next();
				}
			}
		}
		return resultPoint;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * pathの一つ目の値に自分の現在の値が入っていない時修正する
	 *
	 * @param area
	 * @return Listの一番最初を現在のポジションに切り替える
	 * @author 兼近
	 */
	private List<EntityID> modificationPath(List<EntityID>path,EntityID position) {
		if(path!=null&&!path.isEmpty()){
			if(!path.get(0).equals(position)){
				path.add(0,position);
			}
		}else{
			path=new ArrayList<>();
			path.add(position);
		}
		return path;

	}
	//*********************************************************************************************************************************************************************************
	/**
	 * 指定エリアから、エリアの形状から瓦礫の形状を引いたエリアを返す
	 *
	 * @param area
	 * @return 瓦礫を引いたエリア
	 * @author 兼近
	 */
	private java.awt.geom.Area calcAreaSubtractBlockadeArea(Area area) {
		java.awt.geom.Area javaArea=new java.awt.geom.Area(area.getShape());
		javaArea.subtract(calcForbiddenArea(area));
		javaArea.subtract(calcBlockadeForbiddenArea(area));
		/*List<EntityID>blockades=area.getBlockades();
		if(blockades!=null&&!blockades.isEmpty()){
			java.awt.geom.Area blockadeArea=new java.awt.geom.Area();
			for(EntityID blockadeID:blockades){
				Blockade blockade=toBlockade(blockadeID);
				blockadeArea.add(new java.awt.geom.Area(blockade.getShape()));
			}
			javaArea.subtract(blockadeArea);
		}*/
		//exporter.exportLog_Polygon("ab", javaArea, 255, 0, 0, 255);
		return javaArea;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * 指定したエリアからエリア内の進入禁止エリアを返す
	 *
	 * @param area
	 * @return 進入禁止エリア
	 * @author 兼近
	 */
	private java.awt.geom.Area calcForbiddenArea(Area area) {
		java.awt.geom.Area forbiddenAreas_Java=new java.awt.geom.Area();
		List<EntityID> list=area.getNeighbours();
		list.add(area.getID());
		for(EntityID entityID:list){
			Area temp=toArea(entityID);
			List<Edge>edges=temp.getEdges();
			for(Edge edge:edges) {
				if(edge.getNeighbour()==null) {
					Vector2D edgeVector=new Vector2D(edge.getEndX()-edge.getStartX(), edge.getEndY()-edge.getStartY());
					Vector2D edgeVerticalVector_A=new Vector2D(edgeVector.getY(), -edgeVector.getX());
					Vector2D edgeVerticalVector_B=new Vector2D(-edgeVector.getY(), edgeVector.getX());
					Vector2D safetyVerticalVector_A=edgeVerticalVector_A.normalised().scale(forcedDistance);
					Vector2D safetyVerticalVector_B=edgeVerticalVector_B.normalised().scale(forcedDistance);
					Edge safetyEdge_A=new Edge(new Point2D(edge.getStartX()+safetyVerticalVector_A.getX(), edge.getStartY()+safetyVerticalVector_A.getY()), new Point2D(edge.getEndX()+safetyVerticalVector_A.getX(), edge.getEndY()+safetyVerticalVector_A.getY()));
					Edge safetyEdge_B=new Edge(new Point2D(edge.getStartX()+safetyVerticalVector_B.getX(), edge.getStartY()+safetyVerticalVector_B.getY()), new Point2D(edge.getEndX()+safetyVerticalVector_B.getX(), edge.getEndY()+safetyVerticalVector_B.getY()));
					//まさかとは思うが道の幅が0.5mない場合もいちようエリア外になってしまうため何も代入されずnullを吐くことは予想されるっちゃされるがまあないだろう。
					Vector2D modificationVector=edgeVector.normalised().scale(forcedDistance);
					Edge modificationSafetyEdge_A=new Edge(new Point2D(safetyEdge_A.getStartX()-modificationVector.getX(), safetyEdge_A.getStartY()-modificationVector.getY()),new Point2D(safetyEdge_A.getEndX()+modificationVector.getX(), safetyEdge_A.getEndY()+modificationVector.getY()));
					Edge modificationSafetyEdge_B=new Edge(new Point2D(safetyEdge_B.getStartX()-modificationVector.getX(), safetyEdge_B.getStartY()-modificationVector.getY()),new Point2D(safetyEdge_B.getEndX()+modificationVector.getX(), safetyEdge_B.getEndY()+modificationVector.getY()));
					int[] apexX= {modificationSafetyEdge_A.getStartX(),modificationSafetyEdge_A.getEndX(),modificationSafetyEdge_B.getEndX(),modificationSafetyEdge_B.getStartX()};
					int[] apexY= {modificationSafetyEdge_A.getStartY(),modificationSafetyEdge_A.getEndY(),modificationSafetyEdge_B.getEndY(),modificationSafetyEdge_B.getStartY()};
					java.awt.geom.Area forbiddenArea_Java=new java.awt.geom.Area(new java.awt.Polygon(apexX, apexY, 4));
					forbiddenAreas_Java.add(forbiddenArea_Java);
				}
			}
		}
		return forbiddenAreas_Java;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * 指定2頂点を結んだ線を中心線とする幅radiusの長方形を作る
	 * (イメージ) 2頂点を結んだ線に厚みをつけて四角形
	 *
	 * @param startPoint
	 * @param endPoint
	 * @param length
	 * @return 四角形
	 * @author 兼近
	 */
	private java.awt.geom.Area makeArea(Point2D startPoint, Point2D endPoint, double length) {
		Vector2D targetVector=new Vector2D(endPoint.getX()-startPoint.getX(), endPoint.getY()-startPoint.getY());
		Vector2D targetVerticalVector_A=new Vector2D(-targetVector.getY(), targetVector.getX()).normalised().scale(length/2);
		Vector2D targetVerticalVector_B=new Vector2D(targetVector.getY(), -targetVector.getX()).normalised().scale(length/2);
		int[] apexX={(int)(startPoint.getX()+targetVerticalVector_A.getX()), (int)(startPoint.getX()+targetVerticalVector_B.getX()), (int)(startPoint.getX()+targetVerticalVector_B.getX()+targetVector.getX()), (int)(startPoint.getX()+targetVerticalVector_A.getX()+targetVector.getX())};
		int[] apexY={(int)(startPoint.getY()+targetVerticalVector_A.getY()), (int)(startPoint.getY()+targetVerticalVector_B.getY()), (int)(startPoint.getY()+targetVerticalVector_B.getY()+targetVector.getY()), (int)(startPoint.getY()+targetVerticalVector_A.getY()+targetVector.getY())};
		java.awt.geom.Area area=new java.awt.geom.Area(new java.awt.Polygon(apexX, apexY, apexX.length));
		return area;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * Edge群からJavaのArea
	 *
	 * @param edges
	 * @return
	 */
	private java.awt.geom.Area makeArea(List<Edge> edges) {
		if(edges!=null&&!edges.isEmpty()){
			int[] apexX=new int[edges.size()*2];
			int[] apexY=new int[edges.size()*2];
			for(int i=1;i<edges.size()*2;i+=2){
				apexX[i-1]=edges.get(i/2).getStartX();
				apexX[i]=edges.get(i/2).getEndX();
				apexY[i-1]=edges.get(i/2).getStartY();
				apexY[i]=edges.get(i/2).getEndY();
			}
			return new java.awt.geom.Area(new Polygon(apexX, apexY, apexX.length));
		}
		return null;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * 指定の引数を元にJavaのAreaを作成する
	 * @param apexes
	 * @return javaのArea
	 * @author 兼近
	 */
	private java.awt.geom.Area toJavaArea(List<double[]>apexes) {
		int[] apexX=new int[apexes.size()];
		int[] apexY=new int[apexes.size()];
		for(int i=0;i<apexes.size();i++){
			apexX[i]=(int)apexes.get(i)[0];
			apexY[i]=(int)apexes.get(i)[1];
		}
		java.awt.geom.Area area=new java.awt.geom.Area(new java.awt.Polygon(apexX, apexY, apexX.length));
		return area;
	}
	//*********************************************************************************************************************************************************************************

	private java.awt.geom.Area toJavaArea(Point2D point, int radius) {
		int split=60;
		int[] apexX=new int[split];
		int[] apexY=new int[split];
		for(int i=0;i<split;i++){
			apexX[i]=(int)(point.getX()+radius*Math.cos(Math.toRadians(i*6)));
			apexY[i]=(int)(point.getY()+radius*Math.sin(Math.toRadians(i*6)));
		}
		java.awt.geom.Area area=new java.awt.geom.Area(new java.awt.Polygon(apexX, apexY, apexX.length));
		return area;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * 扇型のエリアを作る
	 *
	 * @param area ADFのArea
	 * @return JavaのArea
	 * @author 兼近
	 */
	private java.awt.geom.Area makeArcArea(Area aream,Point2D startPoint, Point2D endPoint, int distance, int radian, int split) {
		Vector2D centralVector=new Vector2D(endPoint.getX()-startPoint.getX(), endPoint.getY()-startPoint.getY());
		Vector2D verticalVectorA=new Vector2D(-centralVector.getY(), centralVector.getX()).normalised().scale(centralVector.getLength()/3);
		Vector2D verticalVectorB=new Vector2D(centralVector.getY(), -centralVector.getX()).normalised().scale(centralVector.getLength()/3);
		int[] apexX={(int)(startPoint.getX()), (int)(startPoint.getX()+centralVector.getX()+verticalVectorA.getX()), (int)(startPoint.getX()+centralVector.getX()+verticalVectorB.getX())};
		int[] apexY={(int)(startPoint.getY()), (int)(startPoint.getY()+centralVector.getY()+verticalVectorA.getY()), (int)(startPoint.getY()+centralVector.getY()+verticalVectorB.getY())};
		java.awt.geom.Area arcArea=new java.awt.geom.Area(new Polygon(apexX, apexY, apexX.length));
		//exporter.exportLog_Polygon("b", arcArea, 0, 255, 0, 100);
		//exporter.exportLog_Line("c", startPoint.getX(), startPoint.getY(), endPoint.getX(), endPoint.getY(), 255, 0, 0, 255);
		java.awt.geom.Area resultArea=new java.awt.geom.Area();
		return resultArea;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * 角度計算(radians)
	 *
	 * @param vector_A
	 * @param vector_B
	 * @return 角度(radian)(0〜π/2)
	 * @author 兼近
	 */
	private double calcAngleR(Vector2D vector_A, Vector2D vector_B) {
		double vector_AB=(vector_A.getX()*vector_B.getX())+(vector_A.getY()*vector_B.getY());
		double vector_AB_Absolute=Math.abs(Math.hypot(vector_A.getX(), vector_A.getY())*Math.hypot(vector_B.getX(), vector_B.getY()));
		double theta=Math.acos(vector_AB/vector_AB_Absolute);
		return theta;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * 角度計算(度)
	 *
	 * @param vector_A
	 * @param vector_B
	 * @return 角度(度)(0〜180)
	 * @author 兼近
	 */
	private double calcAngle(Vector2D vector_A, Vector2D vector_B) {
		double vector_AB=(vector_A.getX()*vector_B.getX())+(vector_A.getY()*vector_B.getY());
		double vector_AB_Absolute=Math.abs(Math.hypot(vector_A.getX(), vector_A.getY())*Math.hypot(vector_B.getX(), vector_B.getY()));
		double theta=Math.acos(vector_AB/vector_AB_Absolute);
		return (theta/Math.PI)*180;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * ADFのAreaから瓦礫のJavaのAreaに変換する
	 *
	 * @param area
	 * @return JavaのArea
	 * @author 兼近
	 */
	private java.awt.geom.Area toBlockadeArea(Area area) {
		java.awt.geom.Area resultArea=new java.awt.geom.Area();
		List<EntityID> blockadeList=area.getBlockades();
		if(blockadeList!=null&&!blockadeList.isEmpty()) {
			for(EntityID blockadeID:blockadeList) {
				Blockade blockade=(Blockade)this.worldInfo.getEntity(blockadeID);
				if(blockade.isApexesDefined()) {
					resultArea.add(new java.awt.geom.Area(blockade.getShape()));
				}
			}
		}
		return resultArea;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * AreaからJavaのAreaに変換する
	 *
	 * @param area
	 * @return JavaのArea
	 * @author 兼近
	 */
	private java.awt.geom.Area toArea(Area area) {
		return new java.awt.geom.Area(area.getShape());
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * EntityIDからADFのAreaに変換する
	 *
	 * @param entityID
	 * @return ADFのArea
	 * @author 兼近
	 */
	private Area toArea(EntityID entityID) {
		if(entityID!=null) {
			StandardEntity standardEntity=this.worldInfo.getEntity(entityID);
			if(standardEntity instanceof Area) {
				return (Area)standardEntity;
			}
		}
		return null;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * EntityIDからADFのBlockadeに変換する
	 *
	 * @param entityID
	 * @return ADFのBlockade
	 * @author 兼近
	 */
	private Blockade toBlockade(EntityID entityID) {
		if(entityID!=null) {
			StandardEntity standardEntity=this.worldInfo.getEntity(entityID);
			if(standardEntity instanceof Blockade) {
				return (Blockade)standardEntity;
			}
		}
		return null;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * EntityIDからADFのHumanに変換する
	 *
	 * @param entityID
	 * @return ADFのHuman
	 * @author 兼近
	 */
	private Human toHuman(EntityID entityID) {
		if(entityID!=null) {
			StandardEntity standardEntity=this.worldInfo.getEntity(entityID);
			if(standardEntity instanceof Human) {
				return (Human)standardEntity;
			}
		}
		return null;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * 指定座標をPoint2Dに変換する
	 *
	 * @param entityID
	 * @return ADFのArea
	 * @author 兼近
	 */
	private Point2D toPoint2D(double x, double y) {
		return new Point2D(x, y);
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * 始点から終点へのベクトルを作る
	 *
	 * @param startPoint
	 * @param endPoint
	 * @return Vector2D
	 * @author 兼近
	 */
	private Vector2D toVector2D(Point2D startPoint, Point2D endPoint) {
		Vector2D targetVector=new Vector2D(endPoint.getX()-startPoint.getX(), endPoint.getY()-startPoint.getY());
		return targetVector;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * 指定座標に指定ベクトルを加算する
	 *
	 * @param point
	 * @param vector
	 * @return 加算済み頂点
	 */
	private Point2D toPoint2D(Point2D point, Vector2D vector) {
		return new Point2D((int)(point.getX()+vector.getX()), (int)(point.getY()+vector.getY()));
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * 指定ポイント方向に向かってシナリオに定義されたClearDistanceにベクトルを再調整してActionClearを返す
	 *
	 * @param targetPoint
	 * @return ActionClear
	 * @author 兼近
	 */
	private Action calcClearAction(Point2D targetPoint) {
		Vector2D targetVector=toVector2D(agentPoint, targetPoint).normalised().scale(this.scenarioInfo.getClearRepairDistance());
		targetPoint=toPoint2D(agentPoint, targetVector);
		this.targetPoint=targetPoint;
		int targetX=(int)targetPoint.getX();
		int targetY=(int)targetPoint.getY();
		//exporter.exportLog_Line(agentPoint, targetPoint, 0, 0, 255, 255);
		return new ActionClear(targetX, targetY);
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * 指定パスを指定エリアまでにカットし、指定座標に向かうActionMoveを返す
	 *
	 * @param targetPoint
	 * @return ActionMove
	 * @author 兼近
	 */
	private Action calcMoveAction(List<EntityID> path, EntityID value,Point2D targetPoint) {
		int index=path.indexOf(value);
		if(index!=-1){
			while(path.size()>index+1){
				path.remove(index+1);
			}
			//exporter.exportLog_Line(agentPoint, targetPoint, 255, 100, 0, 255);
			return new ActionMove(path, (int)targetPoint.getX(), (int)targetPoint.getY());
		}
		//もしも指定のエリアがpathの中に入っていなかった場合はエラー措置で普通にpathのみでMoveさせる
		return new ActionMove(path);
		//return null;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * 指定エリア上の指定ポイントから最も近い移動可能座標を返す
	 * (あくまで瓦礫の無い座標であり、到達可能かどうかは判定しない)
	 *
	 * @param point
	 * @param area
	 * @return 最近点
	 */
	private Point2D modificationMovableNearestPoint(Point2D point, Area area) {
		java.awt.geom.Area javaArea=calcAreaSubtractBlockadeArea(area);
		Point2D resultPoint=null;
		PathIterator pathIterator=javaArea.getPathIterator(null);
		double distance=Integer.MAX_VALUE;
		while(!pathIterator.isDone()) {
			while(!pathIterator.isDone()) {
				double[] points=new double[2];
				int type=pathIterator.currentSegment(points);
				pathIterator.next();
				if(type==PathIterator.SEG_CLOSE) {
					break;
				}
				Point2D tempPoint=new Point2D(points[0], points[1]);
				double tempDistance=calcDistance(point, tempPoint);
				if(tempDistance<distance) {
					distance=tempDistance;
					resultPoint=clonePoint2D(tempPoint);
				}
			}
		}
		return resultPoint;
	}
	//*********************************************************************************************************************************************************************************
	/**
	 * 指定エリアに瓦礫があるか判定する
	 *
	 * @param area
	 * @return true or false
	 * @author 兼近
	 */
	private boolean isBlockadeExist(Area area) {
		return isListContainsContentsEntityID(area.getBlockades());
	}
	//  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■
	private Human detectAgent(Area area){
		Human targetHuman=null;
		List<Human> humanList=this.worldInfo.
				getEntitiesOfType(StandardEntityURN.AMBULANCE_TEAM,StandardEntityURN.FIRE_BRIGADE,StandardEntityURN.POLICE_FORCE,StandardEntityURN.CIVILIAN).
				stream().map(temp->(Human)temp).collect(Collectors.toList());
		if(humanList!=null&&!humanList.isEmpty()){
			List<Human> containsAgent=new ArrayList<>();
			for(Human human:humanList){
				if(human.getPosition().equals(area.getID())&&!human.getID().equals(this.agentInfo.getID())){
					containsAgent.add(human);
				}
			}
			if(containsAgent!=null&&!containsAgent.isEmpty()){
				int distance=Integer.MAX_VALUE;
				for(Human human:containsAgent){
					int tempDistance=(int)Math.hypot(this.agentInfo.getX()-human.getX(), this.agentInfo.getY()-human.getY());
					if(tempDistance<distance){
						distance=tempDistance;
						targetHuman=human;
					}
				}
			}
		}
		//System.out.println("ディテクトエージェント"+targetHuman);
		return targetHuman;
	}
	//  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■
	private Point2D modificationMovePoint(Area area, Point2D startPoint, Point2D endPoint) {
		java.awt.geom.Area area_Java=new java.awt.geom.Area(area.getShape());
		java.awt.geom.Area blockadeArea=calcBlockadeForbiddenArea(area);
		if(blockadeArea!=null) {
			area_Java.subtract(blockadeArea);
			java.awt.geom.Area straightArea=makeArea(startPoint, endPoint, 1);
			straightArea.subtract(blockadeArea);
			if(!straightArea.isSingular()) {
				PathIterator pathIterator=area_Java.getPathIterator(null);
				while(!pathIterator.isDone()) {
					while(!pathIterator.isDone()) {
						double[] points=new double[2];
						int type=pathIterator.currentSegment(points);
						pathIterator.next();
						if(type==PathIterator.SEG_CLOSE) {
							break;
						}
						Point2D tempPoint=new Point2D(points[0], points[1]);
						java.awt.geom.Area relayArea_A=makeArea(startPoint, tempPoint, 3);
						java.awt.geom.Area relayArea_B=makeArea(endPoint, tempPoint, 3);
						relayArea_A.subtract(blockadeArea);
						relayArea_B.subtract(blockadeArea);
						//System.out.println("座標修正した"+endPoint+"を"+tempPoint);
						if(relayArea_A.isSingular()&&relayArea_B.isSingular()) {
							return tempPoint;
						}
					}
				}
			}
		}
		//System.out.println("座標修正せず");
		return endPoint;
	}
	//  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■
	private boolean calcContainsPoint(java.awt.geom.Area area, Point2D point_A, Point2D point_B) {
		PathIterator pathIterator=area.getPathIterator(null);
		while(!pathIterator.isDone()) {
			List<Point2D>pointList=new ArrayList<>();
			while(!pathIterator.isDone()) {
				double[] points=new double[2];
				int type=pathIterator.currentSegment(points);
				pathIterator.next();
				if(type==PathIterator.SEG_CLOSE) {
					break;
				}
				Point2D tempPoint=new Point2D(points[0], points[1]);
				pointList.add(tempPoint);
			}
			if(pointList!=null&&!pointList.isEmpty()) {
				int[] apexX=new int[pointList.size()];
				int[] apexY=new int[pointList.size()];
				for(int i=0;i<pointList.size();i++) {
					Point2D apex=pointList.get(i);
					apexX[i]=(int)apex.getX();
					apexY[i]=(int)apex.getY();
				}
				java.awt.geom.Area tempArea_Java=new java.awt.geom.Area(new java.awt.Polygon(apexX, apexY, apexX.length));
				if(tempArea_Java.intersects(point_A.getX()-1,point_A.getY()-1,3,3)&&tempArea_Java.intersects(point_B.getX()-1,point_B.getY()-1,3,3)) {
					return true;
				}
			}
		}
		return false;
	}
	//  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■
	//座標修正後などに、修正したはいいが目の前を瓦礫が塞いでいる時、修正座標に向かって掘るとすぐ横がもう瓦礫がないエリアがあっても瓦礫を掘ろうとするため
	//そういった時ように座標を修正する
	private Point2D modificationClearPoint(Area area, Point2D clearPoint) {
		java.awt.geom.Area area_Java=new java.awt.geom.Area(area.getShape());
		java.awt.geom.Area blockadeArea=toBlockadeArea(area);
		area_Java.subtract(blockadeArea);
		if(surface(area_Java)>0) {
			java.awt.geom.Area containsArea=calcPointContainsArea(area_Java, clearPoint);
			if(containsArea!=null) {
				PoliceForce agent=(PoliceForce)this.agentInfo.me();
				List<Point2D>pointList=calcAreaPoints(containsArea);
				double surface=Double.MAX_VALUE;
				//double theta=60;
				Point2D efficientPoint=null;
				double distance=Double.MAX_VALUE;
				Vector2D beforeVector=toVector2D(agentPoint, beforeModificationPoint);
				for(Point2D point:pointList) {
					java.awt.geom.Area tempArea=makeArea(agentPoint, point, 1);
					java.awt.geom.Area temp=new java.awt.geom.Area();
					temp.add(tempArea);
					temp.intersect(blockadeArea);
					if(temp.equals(tempArea)){
						//double tempSurface=surface(tempArea);
						double tempDistance=Math.hypot(agent.getX()-point.getX(), agent.getY()-point.getY());
						int tempTheta=(int)calcAngle(beforeVector, toVector2D(agentPoint, point));
						//System.out.println("？ "+tempTheta);
						//exporter.exportLog_Point("clearArea",(int)point.getX(),(int)point.getY(), 255, 0, 0, 255);
						if(2000<tempDistance&&tempDistance<distance&&tempTheta<75) {
							//System.out.println("■条件を満たすから優先");
							return point;
						}
					}else{
						//double tempSurface=surface(tempArea);
						double tempDistance=Math.hypot(agent.getX()-point.getX(), agent.getY()-point.getY());
						int tempTheta=(int)calcAngle(beforeVector, toVector2D(agentPoint, point));
						//System.out.println("？ "+tempTheta);
						//exporter.exportLog_Point("clearArea",(int)point.getX(),(int)point.getY(), 255, 0, 0, 255);
						if(2000<tempDistance&&tempDistance<distance&&tempTheta<75) {
							distance=tempDistance;
							efficientPoint=point;
						}
					}

					/*if(tempSurface<surface&&tempTheta<theta) {
						theta=tempTheta;
						surface=tempSurface;
						efficientPoint=point;
					}*/
				}
				//System.out.println("？？？ "+beforeModificationPoint+","+efficientPoint);
				return efficientPoint;
			}
		}
		return clearPoint;
	}

	//  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■
	//始点と終点からベクトルを作る

	//  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■
	//指定エリア内を構成する頂点を返す便利関数(ただし閉鎖エリアに限る)
	private List<Point2D> calcAreaPoints(java.awt.geom.Area area) {
		List<Point2D>pointList=new ArrayList<>();
		PathIterator pathIterator=area.getPathIterator(null);
		while(!pathIterator.isDone()) {
			while(!pathIterator.isDone()) {
				double[] points=new double[2];
				int type=pathIterator.currentSegment(points);
				pathIterator.next();
				if(type==PathIterator.SEG_CLOSE) {
					break;
				}
				Point2D tempPoint=new Point2D(points[0], points[1]);
				pointList.add(tempPoint);
			}
		}
		return pointList;
	}
	//  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■
	//指定エリア内でポイントを含む閉鎖エリアを返す便利関数
	private java.awt.geom.Area calcPointContainsArea(java.awt.geom.Area area, Point2D point) {
		if(area.isSingular()) {
			return area;
		}
		PathIterator pathIterator=area.getPathIterator(null);
		while(!pathIterator.isDone()) {
			List<Point2D>pointList=new ArrayList<>();
			while(!pathIterator.isDone()) {
				double[] points=new double[2];
				int type=pathIterator.currentSegment(points);
				pathIterator.next();
				if(type==PathIterator.SEG_CLOSE) {
					break;
				}
				Point2D tempPoint=new Point2D(points[0], points[1]);
				pointList.add(tempPoint);
			}
			if(pointList!=null&&!pointList.isEmpty()) {
				int[] apexX=new int[pointList.size()];
				int[] apexY=new int[pointList.size()];
				for(int i=0;i<pointList.size();i++) {
					Point2D apex=pointList.get(i);
					apexX[i]=(int)apex.getX();
					apexY[i]=(int)apex.getY();
				}
				java.awt.geom.Area tempArea=new java.awt.geom.Area(new java.awt.Polygon(apexX, apexY, apexX.length));
				if(tempArea.intersects(point.getX()-1, point.getY()-1,3,3)) {
					return tempArea;
				}
			}
		}
		return new java.awt.geom.Area();
	}
	//  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■
	private List<EntityID> calcPath(EntityID startPosition,EntityID endPosition) {
		List<EntityID>path=pathPlanning.getResult(startPosition, endPosition);
		return path;
	}
	//  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■
	private void calcEdgeBuried(Area area,Edge edge) {
		//これでもあとで使う・・・edgeというよりかはedgeのうちどちらか1点が埋まっていてもう一点は埋まっていないなんかの時のため
		java.awt.geom.Area area_Java=new java.awt.geom.Area(area.getShape());
		java.awt.geom.Area blockadeArea_Java=calcBlockadeForbiddenArea(area);
		area_Java.subtract(blockadeArea_Java);
		Vector2D edgeVector=new Vector2D(edge.getEndX()-edge.getStartX(), edge.getEndY()-edge.getStartY());
		Vector2D edgeVerticalVector_A=new Vector2D(-edgeVector.getY(), edgeVector.getX()).normalised().scale(1);
		Vector2D edgeVerticalVector_B=new Vector2D(edgeVector.getY(), -edgeVector.getX()).normalised().scale(1);
		int[] apexX= {(int)(edge.getStartX()+edgeVerticalVector_A.getX()),(int)(edge.getStartX()+edgeVerticalVector_B.getX()),(int)(edge.getEndX()+edgeVerticalVector_B.getX()),(int)(edge.getEndX()+edgeVerticalVector_A.getX())};
		int[] apexY= {(int)(edge.getStartY()+edgeVerticalVector_A.getY()),(int)(edge.getStartY()+edgeVerticalVector_B.getY()),(int)(edge.getEndY()+edgeVerticalVector_B.getY()),(int)(edge.getEndY()+edgeVerticalVector_A.getY())};
		java.awt.geom.Area EdgeArea_Java=new java.awt.geom.Area(new java.awt.Polygon(apexX, apexY, apexX.length));
	}
	//  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■
	//指定した2頂点が瓦礫に分断されておらず到達可能かどうか
	private boolean calcPassable(Area area_A, Point2D point_A, Area area_B, Point2D point_B) {
		//System.out.println(area_A+","+area_B+","+point_A+","+point_B);
		boolean switchA=false;
		boolean switchB=false;
		java.awt.geom.Area area_A_Java=new java.awt.geom.Area(area_A.getShape());
		java.awt.geom.Area resultArea_A_Java=new java.awt.geom.Area();
		List<EntityID>blockadeList_A=area_A.getBlockades();
		if(blockadeList_A!=null&&!blockadeList_A.isEmpty()) {
			java.awt.geom.Area blockade_A_Java=calcBlockadeForbiddenArea(area_A);
			area_A_Java.subtract(blockade_A_Java);
		}
		PathIterator pathIterator_A=area_A_Java.getPathIterator(null);
		while(!pathIterator_A.isDone()) {
			List<Point2D>pointList_A=new ArrayList<>();
			while(!pathIterator_A.isDone()) {
				double[] points_A=new double[2];
				int type=pathIterator_A.currentSegment(points_A);
				pathIterator_A.next();
				if(type==PathIterator.SEG_CLOSE) {
					break;
				}
				Point2D tempPoint_A=new Point2D(points_A[0], points_A[1]);
				pointList_A.add(tempPoint_A);
			}
			if(pointList_A!=null&&!pointList_A.isEmpty()) {
				int[] apexX_A=new int[pointList_A.size()];
				int[] apexY_A=new int[pointList_A.size()];
				for(int i=0;i<pointList_A.size();i++) {
					Point2D apex_A=pointList_A.get(i);
					apexX_A[i]=(int)apex_A.getX();
					apexY_A[i]=(int)apex_A.getY();
				}
				java.awt.geom.Area tempArea_Java=new java.awt.geom.Area(new java.awt.Polygon(apexX_A, apexY_A, apexX_A.length));
				if(tempArea_Java.intersects(point_A.getX()-1,point_A.getY()-1,3,3)) {
					//System.out.println("ポイントAエリア(先)");
					switchA=true;
					resultArea_A_Java=tempArea_Java;
				}
			}
		}

		java.awt.geom.Area area_B_Java=new java.awt.geom.Area(area_B.getShape());
		java.awt.geom.Area resultArea_B_Java=new java.awt.geom.Area();
		List<EntityID>blockadeList_B=area_B.getBlockades();
		if(blockadeList_B!=null&&!blockadeList_B.isEmpty()) {
			java.awt.geom.Area blockade_B_Java=calcBlockadeForbiddenArea(area_B);
			area_B_Java.subtract(blockade_B_Java);
		}
		PathIterator pathIterator_B=area_B_Java.getPathIterator(null);
		while(!pathIterator_B.isDone()) {
			List<Point2D>pointList_B=new ArrayList<>();
			while(!pathIterator_B.isDone()) {
				double[] points_B=new double[2];
				int type=pathIterator_B.currentSegment(points_B);
				pathIterator_B.next();
				if(type==PathIterator.SEG_CLOSE) {
					break;
				}
				Point2D tempPoint_B=new Point2D(points_B[0], points_B[1]);
				pointList_B.add(tempPoint_B);
			}
			if(pointList_B!=null&&!pointList_B.isEmpty()) {
				int[] apexX_B=new int[pointList_B.size()];
				int[] apexY_B=new int[pointList_B.size()];
				for(int i=0;i<pointList_B.size();i++) {
					Point2D apex_B=pointList_B.get(i);
					apexX_B[i]=(int)apex_B.getX();
					apexY_B[i]=(int)apex_B.getY();
				}
				java.awt.geom.Area tempArea_Java=new java.awt.geom.Area(new java.awt.Polygon(apexX_B, apexY_B, apexX_B.length));
				//exportLog("MovableAreaData", this.time, tempArea_Java,random(0, 255),random(0, 255),random(0, 255),200);
				if(tempArea_Java.intersects(point_B.getX()-1,point_B.getY()-1,3,3)) {
					//System.out.println("ポイントBエリア(元)");
					switchB=true;
					resultArea_B_Java=tempArea_Java;
				}
			}
		}

		java.awt.geom.Area resultArea_Java=new java.awt.geom.Area();
		resultArea_Java.add(resultArea_A_Java);
		resultArea_Java.add(resultArea_B_Java);
		if(switchA&&switchB&&resultArea_Java.isSingular()) {
			//System.out.println("ポイントABエリア(結果)");
			return true;
		}
		return false;
	}
	//  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■
	//指定エリア上の指定座標までの直線上で瓦礫が邪魔しているかどうか
	private boolean calcBlockadeInterruptFront(Area area, Point2D targetPoint) {
		java.awt.geom.Area blockadeArea=new java.awt.geom.Area();
		int clearMaxDistance=this.scenarioInfo.getClearRepairDistance();
		int clearRad=this.scenarioInfo.getClearRepairRad();
		int clearRate=this.scenarioInfo.getClearRepairRate()*1000000;
		int clearDistance=clearRate/(clearRad*2);
		List<EntityID>blockadeList=area.getBlockades();
		if(blockadeList!=null&&!blockadeList.isEmpty()) {
			for(int k=0;k<blockadeList.size();k++) {
				Blockade blockade=(Blockade)this.worldInfo.getEntity(blockadeList.get(k));
				if(blockade.isApexesDefined()) {
					blockadeArea.add(new java.awt.geom.Area(blockade.getShape()));
				}
			}
		}
		PoliceForce agent=(PoliceForce)this.agentInfo.me();
		Vector2D targetVector=new Vector2D(targetPoint.getX()-agent.getX(), targetPoint.getY()-agent.getY()).normalised();
		Vector2D targetVerticalVector_A=new Vector2D(-targetVector.getY(), targetVector.getX()).scale(clearRad/2);
		Vector2D targetVerticalVector_B=new Vector2D(targetVector.getY(), -targetVector.getX()).scale(clearRad/2);
		Vector2D clearVector=new Vector2D(targetVector.getX(), targetVector.getY()).scale(clearDistance);
		int[] apexX={(int)(agent.getX()+targetVerticalVector_A.getX()), (int)(agent.getX()+targetVerticalVector_B.getX()), (int)(agent.getX()+targetVerticalVector_B.getX()+clearVector.getX()), (int)(agent.getX()+targetVerticalVector_A.getX()+clearVector.getX())};
		int[] apexY={(int)(agent.getY()+targetVerticalVector_A.getY()), (int)(agent.getY()+targetVerticalVector_B.getY()), (int)(agent.getY()+targetVerticalVector_B.getY()+clearVector.getY()), (int)(agent.getY()+targetVerticalVector_A.getY()+clearVector.getY())};
		java.awt.geom.Area clearArea=new java.awt.geom.Area(new java.awt.Polygon(apexX, apexY, apexX.length));
		java.awt.geom.Area clearBlockadeArea=new java.awt.geom.Area();
		clearBlockadeArea.add(blockadeArea);
		clearBlockadeArea.intersect(clearArea);
		//exportLog("MovableAreaData", this.time, clearArea,255,0,0,255);
		//System.out.println("途中面積　"+(int)surface(blockadeArea)+","+(int)surface(clearArea));
		double surface=surface(clearBlockadeArea);
		//System.out.println("面積"+(int)surface);
		//exportLog("MovableAreaData", this.time, clearBlockadeArea,0,255,0,255);
		if(surface>0) {
			return true;
		}else {
			clearVector=clearVector.normalised().scale(clearMaxDistance);
			int[] apexX2={(int)(agent.getX()+targetVerticalVector_A.getX()), (int)(agent.getX()+targetVerticalVector_B.getX()), (int)(agent.getX()+targetVerticalVector_B.getX()+clearVector.getX()), (int)(agent.getX()+targetVerticalVector_A.getX()+clearVector.getX())};
			int[] apexY2={(int)(agent.getY()+targetVerticalVector_A.getY()), (int)(agent.getY()+targetVerticalVector_B.getY()), (int)(agent.getY()+targetVerticalVector_B.getY()+clearVector.getY()), (int)(agent.getY()+targetVerticalVector_A.getY()+clearVector.getY())};
			java.awt.geom.Area clearArea2=new java.awt.geom.Area(new java.awt.Polygon(apexX2, apexY2, apexX2.length));
			java.awt.geom.Area clearBlockadeArea2=new java.awt.geom.Area();
			clearBlockadeArea2.add(blockadeArea);
			clearBlockadeArea2.intersect(clearArea2);
			double surface2=surface(clearBlockadeArea2);
			//System.out.println("面積"+surface2/1000000);
			if(surface2>0) {
				return true;
			}
			return false;
		}
	}
	//  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■
	//指定エリア上の指定座標までの直線上で瓦礫が邪魔しているかどうか
	private boolean calcBlockadeInterruptFrontEasy(Area area, Point2D targetPoint) {
		java.awt.geom.Area blockadeArea=new java.awt.geom.Area();
		int clearMaxDistance=this.scenarioInfo.getClearRepairDistance();
		int clearRad=this.scenarioInfo.getClearRepairRad();
		int clearRate=this.scenarioInfo.getClearRepairRate()*1000000;
		int clearDistance=clearRate/(clearRad*2);
		List<EntityID>blockadeList=area.getBlockades();
		if(blockadeList!=null&&!blockadeList.isEmpty()) {
			for(int k=0;k<blockadeList.size();k++) {
				Blockade blockade=(Blockade)this.worldInfo.getEntity(blockadeList.get(k));
				if(blockade.isApexesDefined()) {
					blockadeArea.add(new java.awt.geom.Area(blockade.getShape()));
				}
			}
		}
		PoliceForce agent=(PoliceForce)this.agentInfo.me();
		Vector2D targetVector=new Vector2D(targetPoint.getX()-agent.getX(), targetPoint.getY()-agent.getY()).normalised();
		Vector2D targetVerticalVector_A=new Vector2D(-targetVector.getY(), targetVector.getX()).scale(1);
		Vector2D targetVerticalVector_B=new Vector2D(targetVector.getY(), -targetVector.getX()).scale(1);
		Vector2D clearVector=new Vector2D(targetVector.getX(), targetVector.getY()).scale(clearDistance);
		int[] apexX={(int)(agent.getX()+targetVerticalVector_A.getX()), (int)(agent.getX()+targetVerticalVector_B.getX()), (int)(agent.getX()+targetVerticalVector_B.getX()+clearVector.getX()), (int)(agent.getX()+targetVerticalVector_A.getX()+clearVector.getX())};
		int[] apexY={(int)(agent.getY()+targetVerticalVector_A.getY()), (int)(agent.getY()+targetVerticalVector_B.getY()), (int)(agent.getY()+targetVerticalVector_B.getY()+clearVector.getY()), (int)(agent.getY()+targetVerticalVector_A.getY()+clearVector.getY())};
		java.awt.geom.Area clearArea=new java.awt.geom.Area(new java.awt.Polygon(apexX, apexY, apexX.length));
		java.awt.geom.Area clearBlockadeArea=new java.awt.geom.Area();
		clearBlockadeArea.add(blockadeArea);
		clearBlockadeArea.intersect(clearArea);
		//exportLog("MovableAreaData", this.time, clearArea,255,0,0,255);
		//System.out.println("途中面積　"+(int)surface(blockadeArea)+","+(int)surface(clearArea));
		double surface=surface(clearBlockadeArea);
		//System.out.println("面積"+(int)surface);
		//exportLog("MovableAreaData", this.time, clearBlockadeArea,0,255,0,255);
		if(surface>0) {
			return true;
		}else {
			clearVector=clearVector.normalised().scale(clearMaxDistance);
			int[] apexX2={(int)(agent.getX()+targetVerticalVector_A.getX()), (int)(agent.getX()+targetVerticalVector_B.getX()), (int)(agent.getX()+targetVerticalVector_B.getX()+clearVector.getX()), (int)(agent.getX()+targetVerticalVector_A.getX()+clearVector.getX())};
			int[] apexY2={(int)(agent.getY()+targetVerticalVector_A.getY()), (int)(agent.getY()+targetVerticalVector_B.getY()), (int)(agent.getY()+targetVerticalVector_B.getY()+clearVector.getY()), (int)(agent.getY()+targetVerticalVector_A.getY()+clearVector.getY())};
			java.awt.geom.Area clearArea2=new java.awt.geom.Area(new java.awt.Polygon(apexX2, apexY2, apexX2.length));
			java.awt.geom.Area clearBlockadeArea2=new java.awt.geom.Area();
			clearBlockadeArea2.add(blockadeArea);
			clearBlockadeArea2.intersect(clearArea2);
			double surface2=surface(clearBlockadeArea2);
			if(surface2>4800*2) {
				return true;
			}
			return false;
		}
	}


	//  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■
	//事前計算系
	private void preparingArea() {
		for(StandardEntity area_S:this.worldInfo.getAllEntities().stream().filter(temp->temp instanceof Area).collect(Collectors.toList())) {
			Area area=(Area)area_S;
			java.awt.geom.Area area_Java=new java.awt.geom.Area(area.getShape());
			java.awt.geom.Area forbiddenAreas_Java=new java.awt.geom.Area();
			List<Area> areaList=area.getNeighbours().stream().map(temp->this.worldInfo.getEntity(temp)).map(temp->(Area)temp).collect(Collectors.toList());
			areaList.add(area);
			for(Area areas:areaList) {
				List<Edge>edges=areas.getEdges();
				for(Edge edge:edges) {
					if(edge.getNeighbour()==null) {
						Vector2D edgeVector=new Vector2D(edge.getEndX()-edge.getStartX(), edge.getEndY()-edge.getStartY());
						Vector2D edgeVerticalVector_A=new Vector2D(edgeVector.getY(), -edgeVector.getX());
						Vector2D edgeVerticalVector_B=new Vector2D(-edgeVector.getY(), edgeVector.getX());
						Vector2D safetyVerticalVector_A=edgeVerticalVector_A.normalised().scale(forcedDistance);
						Vector2D safetyVerticalVector_B=edgeVerticalVector_B.normalised().scale(forcedDistance);
						Edge safetyEdge_A=new Edge(new Point2D(edge.getStartX()+safetyVerticalVector_A.getX(), edge.getStartY()+safetyVerticalVector_A.getY()), new Point2D(edge.getEndX()+safetyVerticalVector_A.getX(), edge.getEndY()+safetyVerticalVector_A.getY()));
						Edge safetyEdge_B=new Edge(new Point2D(edge.getStartX()+safetyVerticalVector_B.getX(), edge.getStartY()+safetyVerticalVector_B.getY()), new Point2D(edge.getEndX()+safetyVerticalVector_B.getX(), edge.getEndY()+safetyVerticalVector_B.getY()));
						//Edge safetyEdge=null;
						//まさかとは思うが道の幅が0.5mない場合もいちようエリア外になってしまうため何も代入されずnullを吐くことは予想されるっちゃされるがまあないだろう。
						Vector2D modificationVector=edgeVector.normalised().scale(forcedDistance);
						Edge modificationSafetyEdge_A=new Edge(new Point2D(safetyEdge_A.getStartX()-modificationVector.getX(), safetyEdge_A.getStartY()-modificationVector.getY()),new Point2D(safetyEdge_A.getEndX()+modificationVector.getX(), safetyEdge_A.getEndY()+modificationVector.getY()));
						Edge modificationSafetyEdge_B=new Edge(new Point2D(safetyEdge_B.getStartX()-modificationVector.getX(), safetyEdge_B.getStartY()-modificationVector.getY()),new Point2D(safetyEdge_B.getEndX()+modificationVector.getX(), safetyEdge_B.getEndY()+modificationVector.getY()));
						int[] apexX= {modificationSafetyEdge_A.getStartX(),modificationSafetyEdge_A.getEndX(),modificationSafetyEdge_B.getEndX(),modificationSafetyEdge_B.getStartX()};
						int[] apexY= {modificationSafetyEdge_A.getStartY(),modificationSafetyEdge_A.getEndY(),modificationSafetyEdge_B.getEndY(),modificationSafetyEdge_B.getStartY()};
						java.awt.geom.Area forbiddenArea_Java=new java.awt.geom.Area(new java.awt.Polygon(apexX, apexY, 4));
						forbiddenAreas_Java.add(forbiddenArea_Java);
						//exporter.exportLog_Polygon("ForbiddenAreaData",apexX, apexY,255,100,0,50);
					}
				}
			}
			forbiddenAreas.put(area.getID(), forbiddenAreas_Java);
			area_Java.subtract(forbiddenAreas_Java);
			//safetyAreas.put(area.getID(), area_Java);
		}
	}
	//  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■
	//事前計算系

	private java.awt.geom.Area getForbiddenAreas(Area area) {
		if(!forbiddenAreas.containsKey(area.getID())){
			preparingB(area);
		}
		return forbiddenAreas.get(area.getID());
	}

	/*private java.awt.geom.Area getSafetyAreas(Area area) {
		if(!safetyAreas.containsKey(area.getID())){
			preparingB(area);
		}
		return safetyAreas.get(area.getID());
	}*/

	private void preparingB(Area area) {
		java.awt.geom.Area area_Java=new java.awt.geom.Area(area.getShape());
		java.awt.geom.Area forbiddenAreas_Java=new java.awt.geom.Area();
		List<Area> areaList=area.getNeighbours().stream().map(temp->this.worldInfo.getEntity(temp)).map(temp->(Area)temp).collect(Collectors.toList());
		areaList.add(area);
		for(Area areas:areaList) {
			List<Edge>edges=areas.getEdges();
			for(Edge edge:edges) {
				if(edge.getNeighbour()==null) {
					Vector2D edgeVector=new Vector2D(edge.getEndX()-edge.getStartX(), edge.getEndY()-edge.getStartY());
					Vector2D edgeVerticalVector_A=new Vector2D(edgeVector.getY(), -edgeVector.getX());
					Vector2D edgeVerticalVector_B=new Vector2D(-edgeVector.getY(), edgeVector.getX());
					Vector2D safetyVerticalVector_A=edgeVerticalVector_A.normalised().scale(forcedDistance);
					Vector2D safetyVerticalVector_B=edgeVerticalVector_B.normalised().scale(forcedDistance);
					Edge safetyEdge_A=new Edge(new Point2D(edge.getStartX()+safetyVerticalVector_A.getX(), edge.getStartY()+safetyVerticalVector_A.getY()), new Point2D(edge.getEndX()+safetyVerticalVector_A.getX(), edge.getEndY()+safetyVerticalVector_A.getY()));
					Edge safetyEdge_B=new Edge(new Point2D(edge.getStartX()+safetyVerticalVector_B.getX(), edge.getStartY()+safetyVerticalVector_B.getY()), new Point2D(edge.getEndX()+safetyVerticalVector_B.getX(), edge.getEndY()+safetyVerticalVector_B.getY()));
					//Edge safetyEdge=null;
					//まさかとは思うが道の幅が0.5mない場合もいちようエリア外になってしまうため何も代入されずnullを吐くことは予想されるっちゃされるがまあないだろう。
					Vector2D modificationVector=edgeVector.normalised().scale(forcedDistance);
					Edge modificationSafetyEdge_A=new Edge(new Point2D(safetyEdge_A.getStartX()-modificationVector.getX(), safetyEdge_A.getStartY()-modificationVector.getY()),new Point2D(safetyEdge_A.getEndX()+modificationVector.getX(), safetyEdge_A.getEndY()+modificationVector.getY()));
					Edge modificationSafetyEdge_B=new Edge(new Point2D(safetyEdge_B.getStartX()-modificationVector.getX(), safetyEdge_B.getStartY()-modificationVector.getY()),new Point2D(safetyEdge_B.getEndX()+modificationVector.getX(), safetyEdge_B.getEndY()+modificationVector.getY()));
					int[] apexX= {modificationSafetyEdge_A.getStartX(),modificationSafetyEdge_A.getEndX(),modificationSafetyEdge_B.getEndX(),modificationSafetyEdge_B.getStartX()};
					int[] apexY= {modificationSafetyEdge_A.getStartY(),modificationSafetyEdge_A.getEndY(),modificationSafetyEdge_B.getEndY(),modificationSafetyEdge_B.getStartY()};
					java.awt.geom.Area forbiddenArea_Java=new java.awt.geom.Area(new java.awt.Polygon(apexX, apexY, 4));
					forbiddenAreas_Java.add(forbiddenArea_Java);
					//exporter.exportLog_Polygon("ForbiddenAreaData",apexX, apexY,255,100,0,50);
				}
			}
		}
		forbiddenAreas.put(area.getID(), forbiddenAreas_Java);
		area_Java.subtract(forbiddenAreas_Java);
		//safetyAreas.put(area.getID(), area_Java);
	}



	/**
	 * 指定のJavaのAreaから面積を計算します
	 *
	 * @param area
	 * @return 面積
	 * @author 兼近
	 */
	private double surface(java.awt.geom.Area area) {
		double all=0;
		if(area!=null&&!area.isEmpty()){
			PathIterator pathIterator=area.getPathIterator(null);
			while (!pathIterator.isDone()) {
				List<double[]> points = new ArrayList<double[]>();
				while (!pathIterator.isDone()) {
					double point[]=new double[2];
					int type=pathIterator.currentSegment(point);
					pathIterator.next();
					if (type==PathIterator.SEG_CLOSE) {
						if (points.size()>0)
							points.add(points.get(0));
						break;
					}
					points.add(point);
				}
				double sum=0;
				for (int i=0;i< points.size()-1;i++){
					sum+=(points.get(i)[0]*points.get(i+1)[1])-(points.get(i)[1]*points.get(i+1)[0]);
				}
				all+=Math.abs(sum)/2;
			}
		}
		return all;
	}
	//  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■
	//「便利関数」指定した2点とそれぞれの基準点の距離が最も近いものを返す。ついでに元の点をbeforeとして保存
	private Point2D calcWhichPointNearest(Point2D point_A,Point2D point_B,Point2D basePoint_A,Point2D basePoint_B) {
		double distance_A=Math.hypot(basePoint_A.getX()-point_A.getX(), basePoint_A.getY()-point_A.getY());
		double distance_B=Math.hypot(basePoint_B.getX()-point_B.getX(), basePoint_B.getY()-point_B.getY());
		if(distance_A<distance_B) {
			this.beforeModificationPoint=basePoint_A;
			return point_A;
		}else {
			this.beforeModificationPoint=basePoint_B;
			return point_B;
		}
	}
	//  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■
	//「便利関数」指定した2点とそれぞれの基準点の距離が最も遠いものを返す
	private Point2D calcWhichPointFarthest(Point2D point_A,Point2D point_B,Point2D basePoint_A,Point2D basePoint_B) {
		double distance_A=Math.hypot(basePoint_A.getX()-point_A.getX(), basePoint_A.getY()-point_A.getY());
		double distance_B=Math.hypot(basePoint_B.getX()-point_B.getX(), basePoint_B.getY()-point_B.getY());
		if(distance_A>distance_B) {
			return point_A;
		}else {
			return point_B;
		}
	}
	//  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■
	/**
	 * 指定エリア上の瓦礫のエリアと瓦礫による禁止エリアの両方を返す
	 *
	 * @param area
	 * @return 瓦礫と瓦礫禁止エリア
	 */
	private java.awt.geom.Area calcBlockadeForbiddenArea(Area area) {
		java.awt.geom.Area forbiddenAreas_Java=new java.awt.geom.Area();
		List<EntityID>blockadeList=area.getBlockades();
		if(blockadeList!=null&&!blockadeList.isEmpty()) {
			for(EntityID blockadeID:blockadeList) {
				StandardEntity blockade_S=this.worldInfo.getEntity(blockadeID);
				if(blockade_S instanceof Blockade) {
					Blockade blockade=(Blockade)blockade_S;
					if(blockade.isApexesDefined()) {
						java.awt.geom.Area blockadeArea_Java=new java.awt.geom.Area(blockade.getShape());
						forbiddenAreas_Java.add(blockadeArea_Java);
						PathIterator pathIterator = blockadeArea_Java.getPathIterator(null);
						while (!pathIterator.isDone()) {
							List<Point2D> pointList = new ArrayList<>();
							while (!pathIterator.isDone()) {
								double points[] = new double[2];
								int pathType = pathIterator.currentSegment(points);
								pathIterator.next();
								if (pathType!=PathIterator.SEG_CLOSE) {
									Point2D apex=new Point2D(points[0], points[1]);
									pointList.add(apex);
								}else {
									break;
								}
							}
							if(pointList!=null&&!pointList.isEmpty()) {
								List<Edge>edgeList=new ArrayList<>();
								for(int i=0;i<pointList.size();i++) {
									if(i!=0) {
										Edge edge=new Edge(pointList.get(i-1), pointList.get(i));
										edgeList.add(edge);
									}else {
										Edge edge=new Edge(pointList.get(pointList.size()-1), pointList.get(0));
										edgeList.add(edge);
									}
								}
								if(edgeList!=null&&!edgeList.isEmpty()) {
									for(Edge edge:edgeList) {
										Vector2D edgeVector=new Vector2D(edge.getEndX()-edge.getStartX(), edge.getEndY()-edge.getStartY());
										Vector2D edgeVerticalVector_A=new Vector2D(edgeVector.getY(), -edgeVector.getX());
										Vector2D edgeVerticalVector_B=new Vector2D(-edgeVector.getY(), edgeVector.getX());
										Vector2D safetyVerticalVector_A=edgeVerticalVector_A.normalised().scale(forcedDistance);
										Vector2D safetyVerticalVector_B=edgeVerticalVector_B.normalised().scale(forcedDistance);
										Edge safetyEdge_A=new Edge(new Point2D(edge.getStartX()+safetyVerticalVector_A.getX(), edge.getStartY()+safetyVerticalVector_A.getY()), new Point2D(edge.getEndX()+safetyVerticalVector_A.getX(), edge.getEndY()+safetyVerticalVector_A.getY()));
										Edge safetyEdge_B=new Edge(new Point2D(edge.getStartX()+safetyVerticalVector_B.getX(), edge.getStartY()+safetyVerticalVector_B.getY()), new Point2D(edge.getEndX()+safetyVerticalVector_B.getX(), edge.getEndY()+safetyVerticalVector_B.getY()));
										//Edge safetyEdge=null;
										Point2D safetyEdgeMiddle_A=new Point2D((safetyEdge_A.getStartX()+safetyEdge_A.getEndX())/2, (safetyEdge_A.getStartY()+safetyEdge_A.getEndY())/2);
										Point2D safetyEdgeMiddle_B=new Point2D((safetyEdge_B.getStartX()+safetyEdge_B.getEndX())/2, (safetyEdge_B.getStartY()+safetyEdge_B.getEndY())/2);
										//まさかとは思うが道の幅が0.5mない場合もいちようエリア外になってしまうため何も代入されずnullを吐くことは予想されるっちゃされるがまあないだろう。
										Vector2D modificationVector=edgeVector.normalised().scale(forcedDistance);
										Edge modificationEdge=new Edge(new Point2D(edge.getStartX()-modificationVector.getX(), edge.getStartY()-modificationVector.getY()),new Point2D(edge.getEndX()+modificationVector.getX(), edge.getEndY()+modificationVector.getY()));
										Edge modificationSafetyEdge_A=new Edge(new Point2D(safetyEdge_A.getStartX()-modificationVector.getX(), safetyEdge_A.getStartY()-modificationVector.getY()),new Point2D(safetyEdge_A.getEndX()+modificationVector.getX(), safetyEdge_A.getEndY()+modificationVector.getY()));
										Edge modificationSafetyEdge_B=new Edge(new Point2D(safetyEdge_B.getStartX()-modificationVector.getX(), safetyEdge_B.getStartY()-modificationVector.getY()),new Point2D(safetyEdge_B.getEndX()+modificationVector.getX(), safetyEdge_B.getEndY()+modificationVector.getY()));
										int[] apexX= {modificationSafetyEdge_A.getStartX(),modificationSafetyEdge_A.getEndX(),modificationSafetyEdge_B.getEndX(),modificationSafetyEdge_B.getStartX()};
										int[] apexY= {modificationSafetyEdge_A.getStartY(),modificationSafetyEdge_A.getEndY(),modificationSafetyEdge_B.getEndY(),modificationSafetyEdge_B.getStartY()};
										//exporter.exportLog_Polygon("ForbiddenAreaData", time, "Polygon", apexX, apexY,255,0,0,50);
										java.awt.geom.Area forbiddenArea_Java=new java.awt.geom.Area(new java.awt.Polygon(apexX, apexY, 4));
										forbiddenAreas_Java.add(forbiddenArea_Java);
									}
								}
							}
						}

					}
				}
			}
		}
		return forbiddenAreas_Java;
	}
	//  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■
	//「便利関数」ランダム
	private int random(int start,int end) {
		int result=start;
		if(start!=end) {
			Random random=new Random();
			result=(random.nextInt(end-start+1))+start;
		}
		return result;
	}
}