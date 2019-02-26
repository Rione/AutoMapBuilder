from src import Astar, GraphDrawer, ScenarioReader, MapReader
from src.Method import Greedy
import os.path

distance_table = {}

MAP_NAME = 'sakae'
map = MapReader.MapReader(MAP_NAME)
reader = ScenarioReader.ScenarioReader(MAP_NAME)

world_info = map.build_map()
graph_info = map.build_graph()

scenarios = reader.scenario_reader()


def get_key(start: int, end: int):
    if start > end:
        return 10 ** len(str(start)) * end + start
    else:
        return 10 ** len(str(end)) * start + end


def create_distance_table(location_ids: list):
    astar = Astar.Astar(map.world_info.g_nodes)
    for i in range(len(location_ids)):
        print(i)
        print(distance_table)
        for j in range(len(location_ids)):
            # 同じIDは除外
            if i == j:
                continue
            # hashsetのkey生成
            key = get_key(location_ids[i], location_ids[j])
            # すでに探索済みであれば除外
            if key in distance_table:
                continue
            # Aster計算
            distance = astar.calc_distance(location_ids[i], location_ids[j])[0]
            distance_table.setdefault(key, distance)


def distance(start: int, end: int):
    pass


if __name__ == '__main__':
    location_ids = []

    # 市民の除くエージェント＆避難所をリストアップ
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

    create_distance_table(location_ids)
