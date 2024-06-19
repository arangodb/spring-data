/*
 * DISCLAIMER
 *
 * Copyright 2018 ArangoDB GmbH, Cologne, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright holder is ArangoDB GmbH, Cologne, Germany
 */

package com.arangodb.springframework.testdata.chess.entity;

import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.annotation.From;
import com.arangodb.springframework.annotation.Relations;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.Id;

import java.util.List;

@Document
@Data
@RequiredArgsConstructor
public class Player{
        @Id
        private String id;

        private final String name;

        private final int rating;

        private final String country;

        @Relations(
                edges = Score.class,
                direction = Relations.Direction.OUTBOUND,
                lazy = true
        )
        @EqualsAndHashCode.Exclude
        private List<Tournament> tournaments;

        @From(lazy = true)
        @EqualsAndHashCode.Exclude
        private List<Score> scores;

}
