import numpy as np


class AutoMapBuilder:
    def __init__(self):
        self.nodes = {}
        self.edges = {}
        self.building = {}
        self.road = {}

    def judge_neighbor(self, map_array: np.ndarray, distance: int, target_x: int, target_y: int):
        # 値が入っているかどうか確認
        # 指定場所に値が入っているか
        if not map_array[target_x][target_y] == -1:
            return False

        # xがはみ出していないか
        if target_x - distance >= 0:
            # 上下左右斜めで確認
            if not map_array[target_x - distance][target_y] == -1:
                return False
            if target_y - distance >= 0 and not map_array[target_x - distance][target_y - distance] == -1:
                return False
            if target_y + distance < len(map_array) and not map_array[target_x - distance][target_y + distance] == -1:
                return False

        if target_y - distance >= 0 and not map_array[target_x][target_y - distance] == -1:
            return False
        if target_y + distance < len(map_array) and not map_array[target_x][target_y + distance] == -1:
            return False

        if target_x + distance < len(map_array):
            if not map_array[target_x + distance][target_y] == -1:
                return False
            if target_y - distance >= 0 and not map_array[target_x + distance][target_y - distance] == -1:
                return False
            if target_y + distance < len(map_array) and not map_array[target_x + distance][target_y + distance] == -1:
                return False

        return True

    def can_put_building(self, map_array: np.ndarray, target_x: int, target_y: int):
        # 置けるならid,置けなければすでに入っているid、２つに挟まれている場合は0を返す
        # 指定場所に値が入っているか
        if not map_array[target_x][target_y] == -1:
            return map_array[target_x][target_y]

        building_count = 0
        tmp_id = 0

        # はじめに十字に見る
        if target_x - 1 >= 0:
            if not (map_array[target_x - 1][target_y] == -1 or map_array[target_x - 1][target_y] == 0 or
                    map_array[target_x - 1][target_y] == tmp_id):
                building_count += 1
                tmp_id = map_array[target_x - 1][target_y]

        if target_x + 1 < len(map_array):
            if not (map_array[target_x + 1][target_y] == -1 or map_array[target_x + 1][target_y] == 0 or
                    map_array[target_x + 1][target_y] == tmp_id):
                building_count += 1
                tmp_id = map_array[target_x + 1][target_y]

        if target_y - 1 >= 0:
            if not (map_array[target_x][target_y - 1] == -1 or map_array[target_x][target_y - 1] == 0 or
                    map_array[target_x][target_y - 1] == tmp_id):
                building_count += 1
                tmp_id = map_array[target_x][target_y - 1]

        if target_y + 1 < len(map_array):
            if not (map_array[target_x][target_y + 1] == -1 or map_array[target_x][target_y + 1] == 0 or
                    map_array[target_x][target_y + 1] == tmp_id):
                building_count += 1
                tmp_id = map_array[target_x][target_y + 1]

        if not building_count == 1:
            return 0

        # 斜め
        diagonal_count = 0
        if target_y - 1 >= 0:
            if target_x - 1 >= 0:
                if not (map_array[target_x - 1][target_y - 1] == -1 or map_array[target_x - 1][target_y - 1] == 0 or
                        map_array[target_x - 1][target_y - 1] == tmp_id):
                    diagonal_count += 1
            if target_x + 1 < len(map_array):
                if not (map_array[target_x + 1][target_y - 1] == -1 or map_array[target_x + 1][target_y - 1] == 0 or
                        map_array[target_x + 1][target_y - 1] == tmp_id):
                    diagonal_count += 1

        if target_y + 1 < len(map_array):
            if target_x - 1 >= 0:
                if not (map_array[target_x - 1][target_y + 1] == -1 or map_array[target_x - 1][target_y + 1] == 0 or
                        map_array[target_x - 1][target_y + 1] == tmp_id):
                    diagonal_count += 1
            if target_x + 1 < len(map_array):
                if not (map_array[target_x + 1][target_y + 1] == -1 or map_array[target_x + 1][target_y + 1] == 0 or
                        map_array[target_x + 1][target_y + 1] == tmp_id):
                    diagonal_count += 1

        if building_count == 0 and diagonal_count == 4:
            return 0
        if building_count == 0:
            return -1
        if diagonal_count == 0:
            return tmp_id
        return 0

    def judge_filled(self, array_map: np.ndarray):
        # 全て埋まったか
        for i in range(len(array_map)):
            if -1 in array_map[i]:
                return False
        return True

    def make_map_array(self, number_of_buildings: int, map_width: int, map_height: int):
        building_count = 0
        building_id = 1
        map_array = np.full((map_width, map_height), -1)

        while building_count < number_of_buildings:
            # 配列にランダムに拠点を設定(ただし隣接は禁止)
            # x,yをランダムで選択
            target_x = np.random.randint(len(map_array))
            target_y = np.random.randint(len(map_array))
            # 隣接がないか確認
            if self.judge_neighbor(map_array, 1, target_x, target_y):
                map_array[target_x][target_y] = building_id
                building_id += 1
                building_count += 1

        # 占領開始
        while not self.judge_filled(map_array):
            for i in range(len(map_array)):
                for j in range(len(map_array)):
                    map_array[i][j] = self.can_put_building(map_array, i, j)

        return map_array
