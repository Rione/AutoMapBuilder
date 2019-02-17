import copy
import sys

from src import MapReader, ScenarioReader, GraphDrawer, Astar

# よくばり法
from src.Method import Greedy

if __name__ == '__main__':
    map = MapReader.MapReader('sakae')
    reader = ScenarioReader.ScenarioReader('sakae')

    scenarios = reader.scenario_reader()
    world_info = map.build_map()
    graph_info = map.build_graph()

    astar = Astar.Astar(map.world_info.g_nodes)
    greedy = Greedy.Greedy(map.world_info)
    drawer = GraphDrawer.GraphDrawer(world_info.g_nodes)

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

    result = greedy.calc(location_ids)
    print(result[0])
    route = result[1]

    # 描画用に道のりルートに変換
    result = []
    result.append(route[0])
    # セールスマン問題の近似解を取り出す
    for i in range(len(route) - 1):
        # 最短ルート取得（最初と終端含む）
        a_route = astar.calc_distance(route[i], route[i + 1])[1]
        for r in range(len(a_route) - 1):
            result.append(a_route[r])
    result.append(route[-1])

    drawer.map_register(graph_info.branch_list)
    drawer.route_register(result)
    drawer.show_plt()
