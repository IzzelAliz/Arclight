import java.io._

import scala.annotation.tailrec
import scala.collection.mutable
import scala.io.Source
import scala.util.Using
import scala.util.matching.Regex

object GenSrg extends App {

  val packages = List("it/unimi/dsi/fastutil","org/apache/commons", "jline")

  case class Sig(param: Seq[String], ret: String) {
    override def toString: String = s"(${param.mkString("")})$ret"
    def map(map: Map[String, String]): Sig = {
      val func: String => String = {
        case s"L$cl;" => s"L${map.getOrElse(cl, cl)};"
        case x => x
      }
      val arrayBase = ret.takeWhile(_ == '[')
      val arrayType = ret.dropWhile(_ == '[')
      Sig(param.map(func), arrayBase + func(arrayType))
    }
  }

  object Sig {
    def apply(sig: String): Sig = {
      val s"($param)$ret" = sig
      Sig(read(Seq.empty, param), ret)
    }
    val reg: Regex = "(L[^;]*;)(.*)".r
    @tailrec def read(list: Seq[String], str: String): Seq[String] =
      str match {
        case "" => list
        case reg(a, b) => read(list :+ a, b)
        case x => read(list :+ x.head.toString, x.tail)
      }
  }

  sealed abstract class Mapping
  case class C(notch: String, srg: String) extends Mapping
  case class M(cl: String, notch: String, sig: Sig, srg: String) extends Mapping
  case class F(cl: String, notch: String, srg: String) extends Mapping

  def resource(str: String): Source = Source.fromInputStream(getClass.getResourceAsStream(str))

  @tailrec def readTsrg(map: mutable.Map[C, Seq[Mapping]],
                        lines: Seq[String]): mutable.Map[C, Seq[Mapping]] =
    if (lines.isEmpty) map
    else {
      val Array(notch, srg) = lines.head.split(' ')
      val members = lines.tail.takeWhile(_.head == '\t')
      val mappings = members.map(_.trim split ' ').map({
        case Array(fn, fs) => F(notch, fn, fs)
        case Array(mn, sig, ms) => M(notch, mn, Sig(sig), ms)
      })
      readTsrg(map += (C(notch, srg) -> mappings), lines.tail.dropWhile(_.head == '\t'))
    }

  val bukkit_cl = resource("/bukkit-1.14.4-cl.csrg").getLines()
    .filterNot(_ startsWith "#").filterNot(_.isEmpty)
    .map(_ split ' ').map(it => C(it(0), it(1))).toSeq
  val notch_bukkit = bukkit_cl.map({ case C(n, b) => (n, b) }).toMap
  val bukkit_notch = bukkit_cl.map({ case C(n, b) => (b, n) }).toMap
  val bukkit_nms_prefix = notch_bukkit.values.map({
    case "net/minecraft/server/MinecraftServer" => ("net/minecraft/server/MinecraftServer", "net/minecraft/server/v1_14_R1/MinecraftServer")
    case x => (x, "net/minecraft/server/v1_14_R1/" + x)
  }).toMap
  val bukkit_simple = bukkit_nms_prefix.map { case (a, b) => (b.replace("v1_14_R1/", ""), a) }

  val tsrg = readTsrg(mutable.Map.empty, resource("/joined.tsrg").getLines().toSeq)
  val tsrg_cl = tsrg.keys.map(c => (c.notch, c.srg)).toMap
  val tsrg_members = tsrg.map({ case (c, mapping) => (c.notch, mapping) }).toMap
  val bukkit_srg_cl = bukkit_notch.map({
    case (s, n) => (s, tsrg_cl(n))
  })
  val srg_bukkit_cl = bukkit_srg_cl.map({ case (a, b) => (b, a) })

  val inherit = resource("/inheritanceMap.txt").getLines().map(it => {
    val spl = it.split("\\s")
    (spl(0), spl(1))
  }).toMap.map({
    case (a, b) => (srg_bukkit_cl(a), srg_bukkit_cl(b))
  })

  val bukkit_members_ = notch_bukkit.values.map(x => (x, List[Mapping]())).to(mutable.Map)
  resource("/bukkit-1.14.4-members.csrg").getLines()
    .filterNot(_ startsWith "#").filterNot(_.isEmpty)
    .map(_ split ' ').foreach({
    case Array(c, n, b) =>
      bukkit_members_(c) ::= F(c, n, b)
    case Array(c, n, s, b) =>
      bukkit_members_(c) ::= M(c, n, Sig(s), b)
  })

  def bukkit_members(str: String, parents: Boolean = true): List[Mapping] = {
    val ret = bukkit_members_(str)
    if (!parents) return ret
    inherit.get(str) match {
      case Some(parent) => ret ::: bukkit_members(parent)
      case None => ret
    }
  }

  Using(new PrintWriter(new File("./scripts/bukkit_at.at"))) { writer =>
    try {
      resource("/bukkit-1.14.4.at").getLines()
        .filterNot(_ startsWith "#").filterNot(_.isEmpty)
        .foreach {
          case s"$access $cl_name($param)$ret" =>
            val bukkit_sig = Sig(s"($param)$ret").map(bukkit_simple)
            val i = cl_name.lastIndexOf('/')
            val name = bukkit_simple(cl_name.take(i))
            val method = cl_name.drop(i + 1)
            bukkit_members(name).foreach {
              case M(_, mn, sig, ms) if ms == method && sig == bukkit_sig =>
                val notch_sig = sig.map(bukkit_notch)
                tsrg_members(bukkit_notch(name)).foreach({
                  case M(_, notch, sig, srg) if mn == notch && sig == notch_sig =>
                    writer.println(s"${access.replace("inal", "")} ${tsrg_cl(bukkit_notch(name)).replace('/', '.')} $srg${sig.map(tsrg_cl)}")
                  case _ =>
                })
              case _ =>
            }
          case s"$access $cl" =>
            val count = cl.count(_ == '/')
            val i = cl.lastIndexOf('/')
            if (count == 3) {
              val bukkit = bukkit_simple(cl)
              writer.println(s"$access ${tsrg_cl(bukkit_notch(bukkit)).replace('/', '.')}")
            } else if (count == 4) {
              val field = cl.substring(i).tail
              val bukkit = bukkit_simple(cl.take(i))
              bukkit_members(bukkit, parents = false).foreach({
                case F(_, fn, fs) if fs == field =>
                  tsrg_members(bukkit_notch(bukkit)).foreach({
                    case F(notch, n, srg) if fn == n =>
                      writer.println(s"${access.replace("inal", "")} ${tsrg_cl(notch).replace('/', '.')} $srg")
                    case _ =>
                  })
                case _ =>
              })
            }
        }
    } catch {
      case e: Exception => e.printStackTrace()
    }
  }

  Using(new PrintWriter(new File("./scripts/bukkit_srg.srg"))) { writer =>
    for (pk <- packages) writer.println(s"PK: org/bukkit/craftbukkit/libs/$pk $pk")
    for ((c, mappings) <- tsrg if c.srg.startsWith("net/minecraft") && notch_bukkit.keySet(c.notch.takeWhile(_ != '$'))) {
      val bukkit_cl = notch_bukkit(c.notch)
      writer.println(s"CL: ${bukkit_nms_prefix(bukkit_cl)} ${c.srg}")
      mappings.foreach({
        case F(_, fn, fs) =>
          bukkit_members(bukkit_cl, parents = false).filter({
            case F(_, notch, _) if fn == notch => true
            case _ => false
          }).map({ case F(_, _, fs) => fs }).headOption match {
            case Some(bn) => writer.println(s"FD: ${bukkit_nms_prefix(bukkit_cl)}/$bn ${c.srg}/$fs")
            case None => writer.println(s"FD: ${bukkit_nms_prefix(bukkit_cl)}/$fn ${c.srg}/$fs")
          }
        case M(_, mn, sig, ms) =>
          bukkit_members(bukkit_cl).filter({
            case M(_, notch, bukkit_sig, _) if mn == notch && bukkit_sig.map(bukkit_notch) == sig => true
            case _ => false
          }).map({ case M(_, _, sig, ms) => (sig, ms) }).headOption match {
            case Some((bukkit_sig, bms)) => writer.println(s"MD: ${bukkit_nms_prefix(bukkit_cl)}/$bms ${bukkit_sig.map(bukkit_nms_prefix)} ${c.srg}/$ms ${sig.map(tsrg_cl)}")
            case None => writer.println(s"MD: ${bukkit_nms_prefix(bukkit_cl)}/$mn ${sig.map(notch_bukkit).map(bukkit_nms_prefix)} ${c.srg}/$ms ${sig.map(tsrg_cl)}")
          }
      })
    }
  }
}