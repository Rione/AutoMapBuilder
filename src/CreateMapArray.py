import numpy as np


class CreateMapArray:
    def __init__(self, width: int, height: int, road_width: int):
        self.width = width
        self.height = height
        self.road_width = road_width

    def get_cross_array_data(self, array_map: np.ndarray, x: int, y: int):
        # 配列外の場合-2と空リストを返す
        # 上下左右のみ
        width = array_map.shape[0]
        height = array_map.shape[1]

        def index_out(x: int, y: int):
            if x >= width or x < 0:
                return True
            if y >= height or y < 0:
                return True
            return False

        def register_array_data(x: int, y: int):
            if not index_out(x, y):
                return [-2, []]
            else:
                data = array_map[x][y]
                return [data, [x, y]]

        if index_out(x, y):
            return None

        result = []
        # 上
        result.append(register_array_data(x, y - 1))
        # 右
        result.append(register_array_data(x + 1, y))
        # 下
        result.append(register_array_data(x, y + 1))
        # 左
        result.append(register_array_data(x - 1, y))

        return result

    def judge_neighbor(self, map_array: np.ndarray, distance: int, target_x: int, target_y: int):
        width = map_array.shape[0]
        height = map_array.shape[1]
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
            if target_y + distance < height and not map_array[target_x - distance][target_y + distance] == -1:
                return False

        if target_y - distance >= 0 and not map_array[target_x][target_y - distance] == -1:
            return False
        if target_y + distance < height and not map_array[target_x][target_y + distance] == -1:
            return False

        if target_x + distance < width:
            if not map_array[target_x + distance][target_y] == -1:
                return False
            if target_y - distance >= 0 and not map_array[target_x + distance][target_y - distance] == -1:
                return False
            if target_y + distance < height and not map_array[target_x + distance][target_y + distance] == -1:
                return False

        return True

    def fill_building_id(self, map_array: np.ndarray, target_x: int, target_y: int):
        width = map_array.shape[0]
        height = map_array.shape[1]
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

        if target_x + 1 < width:
            if not (map_array[target_x + 1][target_y] == -1 or map_array[target_x + 1][target_y] == 0 or
                    map_array[target_x + 1][target_y] == tmp_id):
                building_count += 1
                tmp_id = map_array[target_x + 1][target_y]

        if target_y - 1 >= 0:
            if not (map_array[target_x][target_y - 1] == -1 or map_array[target_x][target_y - 1] == 0 or
                    map_array[target_x][target_y - 1] == tmp_id):
                building_count += 1
                tmp_id = map_array[target_x][target_y - 1]

        if target_y + 1 < height:
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
            if target_x + 1 < width:
                if not (map_array[target_x + 1][target_y - 1] == -1 or map_array[target_x + 1][target_y - 1] == 0 or
                        map_array[target_x + 1][target_y - 1] == tmp_id):
                    diagonal_count += 1

        if target_y + 1 < height:
            if target_x - 1 >= 0:
                if not (map_array[target_x - 1][target_y + 1] == -1 or map_array[target_x - 1][target_y + 1] == 0 or
                        map_array[target_x - 1][target_y + 1] == tmp_id):
                    diagonal_count += 1
            if target_x + 1 < width:
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

    def create(self, number_of_buildings: int):
        building_count = 0
        building_id = 1
        map_array = np.full((self.width, self.height), -1)

        while building_count < number_of_buildings:
            # 配列にランダムに拠点を設定(ただし隣接は禁止)
            # x,yをランダムで選択
            target_x = np.random.randint(self.width)
            target_y = np.random.randint(self.height)
            # 隣接がないか確認
            if self.judge_neighbor(map_array, self.road_width, target_x, target_y):
                map_array[target_x][target_y] = building_id
                building_id += 1
                building_count += 1

        # 占領開始
        while not self.judge_filled(map_array):
            for w in range(self.width):
                for h in range(self.height):
                    data = self.fill_building_id(map_array, w, h)
                    map_array[w][h] = data
        return map_array
