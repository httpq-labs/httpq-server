#
# httpq - the webhooks sending server
# Copyright © 2022 Edward Swiac (eswiac@fastmail.com)
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, version 3.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program. If not, see <http://www.gnu.org/licenses/>.
#

# 1st Docker build stage: build the project with Maven
FROM eclipse-temurin:17 as builder
WORKDIR /project
COPY . /project/
RUN ./mvnw package -DskipTests -B

# 2nd Docker build stage: copy builder output and configure entry point
FROM --platform=linux/amd64 eclipse-temurin:17
ENV APP_DIR /application
ENV APP_FILE httpq-server-fat.jar

EXPOSE 8888

WORKDIR $APP_DIR
COPY --from=builder /project/target/httpq-server-fat.jar $APP_DIR/$APP_FILE

ENTRYPOINT ["sh", "-c"]
CMD ["exec java -jar $APP_FILE"]
