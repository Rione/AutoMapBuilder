import os
import sys

import numpy as np

sys.path.append(os.getcwd().replace('/src', ''))

from src.World import Building

MAP_WIDTH = 10
MAP_HEIGHT = 10


# 空白:-1
# 道:0
# 建物:1~

# 最初に配列にランダムに拠点を作る
# その時に別にIDとノード情報を保持するテーブルを作成
# 次に陣地をうばうように建物を広げていく
# その際にノード情報を更新していく(無い座標は追加、ある座標は削除)

class CreateMap:
    def __init__(self):
        self.map_array = np.full((MAP_WIDTH, MAP_HEIGHT), -1)
        self.building_list = []

    def get_key(self, x: int, y: int):
        return 10 ** len(str(y)) * x + y

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
        if not (map_array[target_x][target_y] == -1 or map_array[target_x][target_y] == 0):
            return map_array[target_x][target_y]

        building_count = 0
        tmp_id = 0
        # xがはみ出していないか
        if target_x - 1 >= 0:
            # 上下左右斜めで確認
            if not (map_array[target_x - 1][target_y] == -1 or map_array[target_x - 1][target_y] == 0):
                building_count += 1
                tmp_id = map_array[target_x - 1][target_y]
            if target_y - 1 >= 0 and not (
                    map_array[target_x - 1][target_y - 1] == -1 or map_array[target_x - 1][target_y - 1] == 0):
                building_count += 2
                tmp_id = map_array[target_x - 1][target_y - 1]
            if target_y + 1 < len(map_array) and not (
                    map_array[target_x - 1][target_y + 1] == -1 or map_array[target_x - 1][target_y + 1] == 0):
                building_count += 2
                tmp_id = map_array[target_x - 1][target_y + 1]

        if target_y - 1 >= 0 and not (
                map_array[target_x][target_y - 1] == -1 or map_array[target_x][target_y - 1] == 0):
            building_count += 1
            tmp_id = map_array[target_x][target_y - 1]
        if target_y + 1 < len(map_array) and not (
                map_array[target_x][target_y + 1] == -1 or map_array[target_x][target_y + 1] == 0):
            building_count += 1
            tmp_id = map_array[target_x][target_y + 1]

        if target_x + 1 < len(map_array):
            if not (map_array[target_x + 1][target_y] == -1 or map_array[target_x + 1][target_y] == 0):
                building_count += 1
                tmp_id = map_array[target_x + 1][target_y]
            if target_y - 1 >= 0 and not (
                    map_array[target_x + 1][target_y - 1] == -1 or map_array[target_x + 1][target_y - 1] == 0):
                building_count += 2
                tmp_id = map_array[target_x + 1][target_y - 1]
            if target_y + 1 < len(map_array) and not (
                    map_array[target_x + 1][target_y + 1] == -1 or map_array[target_x + 1][target_y + 1] == 0):
                building_count += 2
                tmp_id = map_array[target_x + 1][target_y + 1]

        if building_count == 0:
            return -1
        if building_count == 1:
            return tmp_id
        return tmp_id

    def judge_filled(self, array_map: np.ndarray):
        # 全て埋まったか
        for i in range(len(array_map)):
            if -1 in array_map[i]:
                return False
        return True

    def create_array_map(self, building_number: int):
        building_id = 1
        building_count = 0

        while building_count < building_number:
            # 配列にランダムに拠点を設定(ただし隣接は禁止)
            # x,yをランダムで選択
            target_x = np.random.randint(10)
            target_y = np.random.randint(10)

            # 隣接がないか確認
            if self.judge_neighbor(self.map_array, 1, target_x, target_y):
                self.map_array[target_x][target_y] = building_id
                self.building_list.append(Building.Building(building_id, target_x, target_y))
                building_id += 1
                building_count += 1

        # 占領開始
        while not self.judge_filled(self.map_array):
            for i in range(len(self.map_array)):
                for j in range(len(self.map_array)):
                    self.map_array[i][j] = self.can_put_building(self.map_array, i, j)


create = CreateMap()
create.create_array_map(15)

print(create.map_array)
