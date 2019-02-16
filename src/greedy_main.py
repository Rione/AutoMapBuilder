import copy
import sys

from src import MapReader, ScenarioReader, GraphDrawer, Astar

# よくばり法
if __name__ == '__main__':
    map = MapReader.MapReader('sakae')
    reader = ScenarioReader.ScenarioReader('sakae')

    scenarios = reader.scenario_reader()
    world_info = map.build_map()
    graph_info = map.build_graph()

    astar = Astar.Astar(map.world_info.g_nodes)
    drawer = GraphDrawer.GraphDrawer(world_info.g_nodes)
    drawer.map_register(graph_info.branch_list)

    # 市民の除くエージェント＆避難所をリストアップ
    location_ids = []
    for scenario in scenarios:
        # 避難所
        if scenario[0] == 'refuge':
            location_ids.append(scenario[1])
        # FB
        if scenario[0] == 'firebrigade':
            location_ids.append(scenario[1])
        # PF
        if scenario[0] == 'policeforce':
            location_ids.append(scenario[1])
        # AT
        if scenario[0] == 'ambulanceteam':
            location_ids.append(scenario[1])

    # 一番近いノードに接続
    # 全てのノードを登録するまでループ
    route = []
    # 未探索ノード登録(先頭は追加しない)
    target_ids = set(copy.deepcopy(location_ids[1:]))
    # スタートを登録
    route.append(location_ids[0])
    total_distance = 0
    while len(target_ids) > 0:
        print(len(route), len(target_ids))
        min = sys.float_info.max
        min_id = 0
        for id in target_ids:
            # 距離取得
            distance = astar.calc_distance(route[-1], id)[0]
            # 最小距離
            if min > distance:
                min = distance
                min_id = id

        # 接続済みのノードを登録
        route.append(min_id)
        # 未探索リストから除外
        target_ids.discard(min_id)
        total_distance += min

    route.append(route[0])

    # 描画用に道のりルートに変換
    for i in range(len(route)):
        
        pass
    drawer.route_register(route)
    drawer.show_plt()
