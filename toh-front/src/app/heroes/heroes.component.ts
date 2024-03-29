import { HeroCreationDto } from '@/appgenerated/model/heroes/hero-creation-dto';
import { Component, OnInit } from '@angular/core';
import { HeroService } from 'src/appgenerated/api/heroes/hero';
import { HeroDto } from 'src/appgenerated/model/heroes/hero-dto';

@Component({
  selector: 'app-heroes',
  templateUrl: './heroes.component.html',
  styleUrls: ['./heroes.component.css'],
})
export class HeroesComponent implements OnInit {
  heroes: HeroDto[] = [];

  constructor(private heroService: HeroService) {}

  ngOnInit(): void {
    this.getHeroes();
  }

  getHeroes(): void {
    this.heroService.getHeroes().subscribe((heroes) => (this.heroes = heroes));
  }

  add(name: string): void {
    name = name.trim();
    if (!name) {
      return;
    }
    this.heroService.addHero({ name } as HeroCreationDto).subscribe((hero) => {
      this.heroes.push(hero);
    });
  }

  delete(hero: HeroDto): void {
    this.heroes = this.heroes.filter((h) => h !== hero);
    this.heroService.deleteHero(hero.id!).subscribe(() => this.getHeroes());
  }
}
