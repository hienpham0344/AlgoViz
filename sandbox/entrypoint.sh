#!/bin/sh
set -eu
export HOME=/work TMPDIR=/tmp
# No caller source is evaluated as shell. All command operands stay separate arguments.
mode="$1"; shift
if [ "$mode" = trace ]; then
  exec timeout --signal=KILL 35s java -Xmx128m -XX:ActiveProcessorCount=1 \
    -cp '/opt/ast/classes:/opt/ast/lib/*' com.example.astchunker.sandbox.TraceWorker /input/source.java
fi
[ "$mode" = run ] || exit 64
language="$1"; filename="$2"; main="$3"; milliseconds="$4"; heap="$5"; stack="$6"
cp "/input/$filename" "/work/$filename"
seconds=$(awk -v ms="$milliseconds" 'BEGIN {printf "%.3f", ms / 1000}')
compile_failed() { echo 'ALGOVIZ_COMPILATION_ERROR' >&2; exit 65; }
case "$language" in
  java-source)
    exec timeout --signal=KILL "${seconds}s" java "-Xmx${heap}m" "-Xss${stack}k" -XX:ActiveProcessorCount=1 "/work/$filename" < /input/stdin.txt ;;
  java)
    timeout --signal=KILL 10s javac -J-Xmx128m -J-XX:ActiveProcessorCount=1 -proc:none --release 17 -encoding UTF-8 -d /work "/work/$filename" || compile_failed
    exec timeout --signal=KILL "${seconds}s" java "-Xmx${heap}m" "-Xss${stack}k" -XX:ActiveProcessorCount=1 -cp /work "$main" < /input/stdin.txt ;;
  python|py) exec timeout --signal=KILL "${seconds}s" python3 -I "/work/$filename" < /input/stdin.txt ;;
  javascript|js) exec timeout --signal=KILL "${seconds}s" node --max-old-space-size=128 "/work/$filename" < /input/stdin.txt ;;
  cpp|c++)
    timeout --signal=KILL 10s g++ -std=c++17 "/work/$filename" -o /work/program || compile_failed
    exec timeout --signal=KILL "${seconds}s" /work/program < /input/stdin.txt ;;
  *) exit 64 ;;
esac
